package com.example.bysjapp;

import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DetectionFragment extends Fragment {

    // 蓝牙相关UUID (根据CH9143文档)
    private static final UUID SERVICE_UUID = UUID.fromString("0000FFF0-0000-1000-8000-00805F9B34FB");
    private static final UUID NOTIFY_UUID = UUID.fromString("0000FFF1-0000-1000-8000-00805F9B34FB");
    private static final UUID WRITE_UUID = UUID.fromString("0000FFF2-0000-1000-8000-00805F9B34FB");

    // UI控件
    private Button btnConnectDevice;
    private TextView tvConnectedDevice;
    private TextView tvDisplayValue;
    private LineChart lineChart;
    private Button btnStartDetection;
    private TextView tvTestDuration;
    private TextView tvMaxValue;
    private TextView tvMinValue;
    private TextView tvAvgValue;

    // 新增标题栏按钮
    private Button btnMeasurement;
    private Button btnHistory;

    // 蓝牙相关
    private boolean isDeviceConnected = false;
    private final Handler handler = new Handler();
    private Runnable updateChartRunnable;
    private Runnable mockDataRunnable;
    private Runnable countdownRunnable;

    // 随机数生成器
    private Random random = new Random();

    // 检测状态
    private boolean isDetecting = false;
    private boolean detectionStopped = false;
    private long testStartTime = 0;
    private long dataStartTime = 0; // 数据开始显示的时间
    private boolean isDataVisible = false; // 数据是否可见（5秒后）

    // 数据统计
    private float maxValue = Float.MIN_VALUE;
    private float minValue = Float.MAX_VALUE;
    private float sumValue = 0;
    private int dataCount = 0;

    // 数据库
    private BloodSugarDatabaseHelper dbHelper;

    // 权限请求码
    private static final int REQUEST_BLUETOOTH_PERMISSIONS = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    private long latestRecordId; // 新增成员变量，用于保存最新的 recordId

    // 在DetectionFragment类中添加
    private TextView tvDetectionAnalysis; // 分析结果显示TextView
    private Button btnFetchAnalysis;     // 获取分析按钮
    private String analysisPrompt;       // 构造的提示词

    private static final String BASE_URL = "http://10.0.2.2:5000"; // 后端API地址
    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient client;

    // 常量定义
    private static final long DELAY_BEFORE_DISPLAY = 5000; // 5秒延迟（毫秒）
    private static final long UPDATE_INTERVAL_MS = 1000L; // 数据更新间隔

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detection, container, false);
        dbHelper = new BloodSugarDatabaseHelper(getContext());

        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        // 初始化UI
        initViews(view);
        setupChart();

        return view;
    }

    private void initViews(View view) {
        btnConnectDevice = view.findViewById(R.id.btn_connect_device);
        tvConnectedDevice = view.findViewById(R.id.tv_connected_device);
        tvDisplayValue = view.findViewById(R.id.tv_display_value);
        lineChart = view.findViewById(R.id.line_chart);
        btnStartDetection = view.findViewById(R.id.btn_start_detection);
        tvTestDuration = view.findViewById(R.id.tv_test_duration);
        tvMaxValue = view.findViewById(R.id.tv_max_value);
        tvMinValue = view.findViewById(R.id.tv_min_value);
        tvAvgValue = view.findViewById(R.id.tv_avg_value);

        // 初始化统计数据显示
        resetStatsDisplay();

        btnConnectDevice.setOnClickListener(v -> {
            // 直接显示模拟的设备连接对话框
            showMockBluetoothDeviceDialog();
        });

        // 历史记录按钮
        Button btnHistoryRecord = view.findViewById(R.id.btn_history_record);
        if (btnHistoryRecord != null) {
            btnHistoryRecord.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), HistoryActivity.class);
                startActivity(intent);
            });
        } else {
            Log.e("DetectionFragment", "btn_history_record is null in fragment_detection.xml");
        }

        // 检测按钮
        btnStartDetection.setOnClickListener(v -> handleDetectionButtonClick());

        // 添加新控件的初始化
        tvDetectionAnalysis = view.findViewById(R.id.tv_detection_analysis);
        btnFetchAnalysis = view.findViewById(R.id.btn_fetch_analysis);

        // 设置获取分析按钮点击事件
        btnFetchAnalysis.setOnClickListener(v -> fetchAnalysisFromAPI(latestRecordId));

        // 初始化时显示等待提示
        showWaitingUI();
    }

    private void handleDetectionButtonClick() {
        if (!isDeviceConnected) {
            Toast.makeText(getActivity(), "蓝牙未连接！", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isDetecting) {
            stopChartUpdate();
            detectionStopped = true;
            btnStartDetection.setBackgroundColor(android.graphics.Color.GRAY);
            btnStartDetection.setText("重新检测");
        } else if (detectionStopped) {
            resetDetection();
        } else {
            startChartUpdate();
            btnStartDetection.setBackgroundColor(android.graphics.Color.GREEN);
            btnStartDetection.setText("停止检测");
        }
    }

    private void showMockBluetoothDeviceDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getActivity());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_device_list, null);
        builder.setView(dialogView);
        final android.app.AlertDialog dialog = builder.create();

        ListView lvDeviceList = dialogView.findViewById(R.id.lv_device_list);
        List<String> deviceList = new ArrayList<>();
        // 直接添加模拟的设备信息
        deviceList.add("CH9143BLE2U\n50:54:7B:5A:DF:A7");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, deviceList);
        lvDeviceList.setAdapter(adapter);

        lvDeviceList.setOnItemClickListener((parent, view, position, id) -> {
            // 模拟设备连接
            simulateDeviceConnection();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void simulateDeviceConnection() {
        isDeviceConnected = true;
        // 模拟连接成功的UI更新
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(getActivity(), "已连接: CH9143BLE2U", Toast.LENGTH_SHORT).show();
            tvConnectedDevice.setText("已连接设备: CH9143BLE2U");
            btnConnectDevice.setText("断开连接");
            btnConnectDevice.setOnClickListener(v -> disconnectDevice());
        });
    }

    private void startChartUpdate() {
        isDetecting = true;
        testStartTime = System.currentTimeMillis();
        dataStartTime = testStartTime + DELAY_BEFORE_DISPLAY; // 5秒后开始显示数据
        isDataVisible = false;

        // 重置统计数据
        maxValue = Float.MIN_VALUE;
        minValue = Float.MAX_VALUE;
        sumValue = 0;
        dataCount = 0;

        // 显示等待界面
        showWaitingUI();

        // 启动倒计时显示
        startCountdownDisplay();

        // 开始生成模拟数据（立即开始，但5秒后才显示）
        mockDataRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isDetecting) return;

                // 生成5.0-6.0之间的随机血糖值
                float glucoseValue = 5.0f + random.nextFloat() * 1.0f;
                // 模拟接收到数据
                handleMockData(glucoseValue);
                handler.postDelayed(this, UPDATE_INTERVAL_MS);
            }
        };
        handler.post(mockDataRunnable);
    }

    private void startCountdownDisplay() {
        countdownRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isDetecting) return;

                long currentTime = System.currentTimeMillis();
                long remainingTime = dataStartTime - currentTime;

                if (remainingTime <= 0) {
                    // 5秒倒计时结束，开始显示数据
                    isDataVisible = true;
                    hideWaitingUI();
                    showDataDisplay();
                } else {
                    // 更新倒计时显示
                    updateCountdownUI(remainingTime);
                    handler.postDelayed(this, 100); // 每100毫秒更新一次
                }
            }
        };
        handler.post(countdownRunnable);
    }

    private void showWaitingUI() {
        requireActivity().runOnUiThread(() -> {
            tvDisplayValue.setText("准备中...");
            tvTestDuration.setText("数据将在5秒后开始显示");
            // 可以添加其他等待状态的UI设置
        });
    }

    private void updateCountdownUI(long remainingMs) {
        requireActivity().runOnUiThread(() -> {
            long remainingSeconds = (remainingMs + 999) / 1000; // 向上取整
            String countdownText = String.format("数据将在 %d 秒后开始显示...", remainingSeconds);
            tvTestDuration.setText(countdownText);
        });
    }

    private void hideWaitingUI() {
        requireActivity().runOnUiThread(() -> {
            // 清除等待状态显示
            tvTestDuration.setText("0s");
        });
    }

    private void showDataDisplay() {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(getActivity(), "开始显示血糖数据", Toast.LENGTH_SHORT).show();
            // 可以添加数据开始显示的动画效果
        });
    }

    private void handleMockData(float glucoseValue) {
        // 更新统计数据（无论是否显示都要记录）
        if (glucoseValue > maxValue) maxValue = glucoseValue;
        if (glucoseValue < minValue) minValue = glucoseValue;
        sumValue += glucoseValue;
        dataCount++;

        // 只有在5秒后才更新UI显示
        if (isDataVisible) {
            requireActivity().runOnUiThread(() -> {
                addChartEntry(glucoseValue);
                tvDisplayValue.setText(String.format(Locale.getDefault(), "%.2f", glucoseValue));

                // 更新统计数据显示
                tvMaxValue.setText(String.format(Locale.getDefault(), "%.2f", maxValue));
                tvMinValue.setText(String.format(Locale.getDefault(), "%.2f", minValue));
                tvAvgValue.setText(String.format(Locale.getDefault(), "%.2f", sumValue / dataCount));

                // 更新检测时长显示
                long duration = (System.currentTimeMillis() - dataStartTime) / 1000;
                tvTestDuration.setText(duration + "s");
            });
        }
    }

    private void stopChartUpdate() {
        isDetecting = false;
        isDataVisible = false;

        if (mockDataRunnable != null) {
            handler.removeCallbacks(mockDataRunnable);
        }
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }

        calculateAndSaveStats();
        btnStartDetection.setText("重新检测");
        // 构造分析提示词
        buildAnalysisPrompt();
        // 自动获取分析结果
        long recordId = saveToDatabase(sumValue / dataCount, maxValue, minValue);
        latestRecordId = recordId; // 更新 latestRecordId
        fetchAnalysisFromAPI(recordId);
    }

    private void buildAnalysisPrompt() {
        String duration = tvTestDuration.getText().toString();
        String max = tvMaxValue.getText().toString();
        String min = tvMinValue.getText().toString();
        String avg = tvAvgValue.getText().toString();

        analysisPrompt = "根据以下血糖检测数据进行分析：\n" +
                "检测时长：" + duration + "\n" +
                "最大值：" + max + " mM\n" +
                "最小值：" + min + " mM\n" +
                "平均值：" + avg + " mM\n\n" +
                "请以糖尿病专家的身份，分析这些血糖数据是否正常，" +
                "可能存在什么问题，并给出饮食、运动、用药和生活方式建议。" +
                "回答请简明扼要，控制在300字以内。";

        // 启用获取分析按钮
        btnFetchAnalysis.setEnabled(true);
    }

    private void fetchAnalysisFromAPI(long recordId) {
        if (analysisPrompt == null || analysisPrompt.isEmpty()) {
            Toast.makeText(getActivity(), "请先完成检测", Toast.LENGTH_SHORT).show();
            return;
        }

        // 显示加载状态
        tvDetectionAnalysis.setText("分析中...");
        btnFetchAnalysis.setEnabled(false);

        try {
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("question", analysisPrompt);

            RequestBody body = RequestBody.create(jsonBody.toString(), JSON_MEDIA_TYPE);
            Request request = new Request.Builder()
                    .url(BASE_URL + "/api/chat")
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    requireActivity().runOnUiThread(() -> {
                        tvDetectionAnalysis.setText("分析失败: " + e.getMessage());
                        btnFetchAnalysis.setEnabled(true);
                    });
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        requireActivity().runOnUiThread(() -> {
                            tvDetectionAnalysis.setText("服务器响应错误: " + response.code());
                            btnFetchAnalysis.setEnabled(true);
                        });
                        return;
                    }
                    try {
                        String responseBody = response.body() != null ? response.body().string() : "";
                        JSONObject json = new JSONObject(responseBody);
                        if (json.has("success") && json.getBoolean("success")) {
                            String analysis = json.getString("final_answer");
                            requireActivity().runOnUiThread(() -> {
                                tvDetectionAnalysis.setText(analysis);
                                // 保存分析结果到数据库
                                saveAnalysisToDatabase(recordId, analysis);
                            });
                        } else {
                            requireActivity().runOnUiThread(() -> {
                                tvDetectionAnalysis.setText("无效的响应格式");
                            });
                        }
                    } catch (JSONException e) {
                        requireActivity().runOnUiThread(() -> {
                            tvDetectionAnalysis.setText("数据解析失败");
                        });
                    } finally {
                        requireActivity().runOnUiThread(() -> {
                            btnFetchAnalysis.setEnabled(true);
                        });
                    }
                }
            });
        } catch (JSONException e) {
            tvDetectionAnalysis.setText("构建请求失败");
            btnFetchAnalysis.setEnabled(true);
        }
    }

    private long saveToDatabase(float average, float max, float min) {
        android.database.sqlite.SQLiteDatabase db = dbHelper.getWritableDatabase();
        String timestamp = String.valueOf(System.currentTimeMillis());
        ContentValues values = new ContentValues();
        values.put("timestamp", timestamp);
        values.put("average_value", average);
        values.put("max_value", max);
        values.put("min_value", min);
        long newRowId = db.insert("BloodSugarRecords", null, values);
        db.close();
        return newRowId;
    }

    private void saveAnalysisToDatabase(long recordId, String analysis) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("analysis_text", analysis);
        db.update("BloodSugarRecords", values, "id = ?", new String[]{String.valueOf(recordId)});
        db.close();
    }

    private void resetDetection() {
        isDataVisible = false;
        lineChart.clear();
        setupChart();
        lineChart.getXAxis().setAxisMaximum(20);
        lineChart.invalidate();
        detectionStopped = false;
        btnStartDetection.setBackgroundColor(android.graphics.Color.GRAY);
        btnStartDetection.setText("点击开始检测");

        resetStatsDisplay();
        // 重置分析区域
        tvDetectionAnalysis.setText("检测完成后将显示分析结果...");
        btnFetchAnalysis.setEnabled(false);
        analysisPrompt = null;

        // 显示初始等待状态
        showWaitingUI();
    }

    private void resetStatsDisplay() {
        tvTestDuration.setText("0s");
        tvMaxValue.setText("0");
        tvMinValue.setText("0");
        tvAvgValue.setText("0");
        tvDisplayValue.setText("0.00");
    }

    private void calculateAndSaveStats() {
        long testDurationMillis = System.currentTimeMillis() - testStartTime;
        int testDurationSec = (int) (testDurationMillis / 1000);
        tvTestDuration.setText(testDurationSec + "s");

        if (dataCount > 0) {
            float average = sumValue / dataCount;
            tvMaxValue.setText(String.format(Locale.getDefault(), "%.2f", maxValue));
            tvMinValue.setText(String.format(Locale.getDefault(), "%.2f", minValue));
            tvAvgValue.setText(String.format(Locale.getDefault(), "%.2f", average));

            // 保存到数据库
            saveToDatabase(average, maxValue, minValue);
        }
    }

    private void addChartEntry(float value) {
        LineData data = lineChart.getData();
        if (data != null) {
            ILineDataSet dynamicSet = data.getDataSetByIndex(1);
            if (dynamicSet == null) {
                dynamicSet = createSet();
                data.addDataSet(dynamicSet);
            }

            int currentX = dynamicSet.getEntryCount();
            data.addEntry(new Entry(currentX, value), 1);

            // 更新阴影区域
            ILineDataSet shadowSet = data.getDataSetByIndex(0);
            if (shadowSet != null && shadowSet.getEntryCount() >= 2) {
                Entry secondEntry = shadowSet.getEntryForIndex(1);
                secondEntry.setX(Math.max(20, currentX));
            }

            data.notifyDataChanged();
            lineChart.notifyDataSetChanged();
            lineChart.setVisibleXRangeMaximum(20);
            lineChart.moveViewToX(data.getEntryCount());
        }
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);

        YAxis yAxisLeft = lineChart.getAxisLeft();
        yAxisLeft.setAxisMinimum(3.0f);
        yAxisLeft.setAxisMaximum(10.0f);
        yAxisLeft.setLabelCount(4, true);
        yAxisLeft.setDrawGridLines(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(5f);
        xAxis.setLabelCount(5, true);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "s";
            }
        });
        xAxis.setDrawGridLines(false);

        // 创建固定阴影数据集（用于填充4.0-8.0区域）
        LineDataSet shadowSet = createShadowSet();
        // 创建动态曲线数据集
        LineDataSet dynamicSet = createSet();

        LineData data = new LineData();
        data.addDataSet(shadowSet);
        data.addDataSet(dynamicSet);
        lineChart.setData(data);
    }

    private LineDataSet createShadowSet() {
        ArrayList<Entry> entries = new ArrayList<>();
        entries.add(new Entry(0, 8.0f));
        entries.add(new Entry(20, 8.0f));
        LineDataSet set = new LineDataSet(entries, " ");
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setColor(android.graphics.Color.TRANSPARENT);
        set.setDrawFilled(true);
        set.setFillColor(android.graphics.Color.LTGRAY);
        set.setFillAlpha(80);
        set.setFillFormatter((dataSet, dataProvider) -> 4.0f);
        return set;
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "血糖值");
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(android.graphics.Color.BLUE);
        set.setLineWidth(2f);
        set.setDrawCircles(false);
        set.setDrawValues(false);
        set.setDrawFilled(false);
        return set;
    }

    private void disconnectDevice() {
        isDeviceConnected = false;
        isDetecting = false;
        isDataVisible = false;

        requireActivity().runOnUiThread(() -> {
            tvConnectedDevice.setText("未连接");
            btnConnectDevice.setText("连接设备");
            btnConnectDevice.setOnClickListener(v -> {
                showMockBluetoothDeviceDialog();
            });

            if (isDetecting) {
                stopChartUpdate();
            }

            // 清理UI状态
            resetStatsDisplay();
            showWaitingUI();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (updateChartRunnable != null) {
            handler.removeCallbacks(updateChartRunnable);
        }
        if (mockDataRunnable != null) {
            handler.removeCallbacks(mockDataRunnable);
        }
        if (countdownRunnable != null) {
            handler.removeCallbacks(countdownRunnable);
        }
        disconnectDevice();
    }
}