# QSBPS
This is the JAVA code for the CIKM'19 publication "Learning to Ask: Question-based Sequential Bayesian Product Search"

Input:

Training set: trainDir, each file is a topic id, each line is a product id. Few examples are in the folder "qrelDirectoryTrain".

Testing set: testingDir, each file is a topic id, each line is a product id. Few examples are in the folder "qrelDirectoryTesting" .

Question pool: pathin, the entity file from TagMe, each file is a product id with its corresponding extracted entities. Examples are in the file "doc-entity-***.txt".

Predefined number of questions: iteration1.

Predefined weight of question reward: rewardParameter.

Output:

Ranking results:resultDir. each file is a topic id, each line is in the form "ranking score=product id", Descending order. 

After getting the ranking results, you can calculate the evaluation metrics using evaluation tools (e.g., pytrec_eval) or calculate by yourself.



