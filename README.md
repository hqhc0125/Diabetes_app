本项目的开发环境如下：
1、操作系统：Windows 10

2、开发平台/环境版本：
1）Android studio：
Android Studio Iguana | 2023.2.1
Build #AI-232.10227.8.2321.11479570, built on February 22, 2024
Runtime version: 17.0.9+0--11185874 amd64
IntelliJ IDEA 2022.3.3(Ultimate Edition)
2）Pycharm 2023.1(Community Edition)
3）Neo4j：neo4j-community-4.4.32
4）深度学习框架：Pytorch

3、运行方法：
1）Android Studio打开“bysjAPP”，直接运行，APP账号：1，密码：1；
2）VScode打开back_code，需先配置必要的库；
3）知识图谱：运行“version1\KG”，即可构建知识图谱，终端打开neo4j即可查看知识图谱，注意端口和账号密码；
4）意图检测：运行"version1\intent_data_process"对数据进行预处理，之后可分别运行"version1\intent_detection_textCNN",
"version1\intent_detection_Bi-LSTM","version1\intent_detection_Bi-LSTM_with_attention",
"version2\fine-turning\intent_detection_electra",四个文件，进行四个深度学习模型训练，比较意图检测的问题分类性能；
5）运行"version2\app"同时保持运行状态，即可保证后端处于打开状态，
注意：可能需先运行"version2\entity”，"version2\intent_detection",""version2\RAG"和""version2\Web_KGshow"四个代码文件；
6）后端运行需访问huggingface，访问较慢，可想办法解决；
7）注意代码文件中所有涉及到数据集文件或模型文件本地保存地址的地方，需修改为实际地址，
有的模型过大，可自行到huggingface上搜索下载；
8）大语言模型选择的是智谱的glm-4-flash，API key可在其官网免费申请；

