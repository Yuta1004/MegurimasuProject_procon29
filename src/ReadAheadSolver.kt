import java.lang.IndexOutOfBoundsException
import java.util.Random;
import kotlin.math.max
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

val random = Random()

fun main(args: Array<String>){
    val agentPos = getAgentPosFromQR()
    val scoreData = getScoreDataFromQR()
    val megurimasu = MegurimasuSimulator(agentPos, scoreData)

//    // 速度計算用
//    for(i: Int in 0 until 100) {
//        var result = Pair(0, mapOf("Null" to 0))
//        val time = measureTimeMillis { result = searchBestBehavior(megurimasu, 3, arrayOf(0, 0, 5)) }
//        val (maxScore, bestBehavior) = result
//
//        println("Time: $time ms")
//        println("MaxScore: $maxScore")
//        println("BestBehavior: A_1 -> ${bestBehavior["A_1"]}, A_2 -> ${bestBehavior["A_2"]}")
//        println()
//        cnt = 0
//    }
}

// 再帰でより良い手を探す
fun searchBestBehavior(megurimasu: MegurimasuSimulator, depth: Int, probability: Array<Int>): Pair<Int, Map<String, Int>>{
    // 葉ならスコアを計算して返す
    if(depth == 0){
        val score = megurimasu.calScore()
        return Pair(score["A"]!! - score["B"]!!, mapOf())
    }

    // 次の手を列挙
    val agentsAction = listOf("A_1", "A_2", "B_1", "B_2")
            .map{ agentName ->
                val bruteforce = strategyOfBruteForce(megurimasu, agentName, probability[0])
                val stalker = strategyOfStalker(megurimasu, agentName, probability[1])
                 val prayToGod = strategyOfPrayToGod(megurimasu, agentName, probability[2])

                agentName to bruteforce + stalker + prayToGod
            }
            .toMap()

    // それぞれのエージェントが選択した手を合わせて次の盤面を決める
    val nextBehaviors = arrayListOf<Map<String, Int>>()
    val total = probability.sum()
    for(i: Int in 0 until total * total){
        nextBehaviors.add(mapOf(
                "A_1" to agentsAction["A_1"]!![i / total],
                "A_2" to agentsAction["A_2"]!![i % total],
                "B_1" to agentsAction["B_1"]!![i / total],
                "B_2" to agentsAction["B_2"]!![i % total]
        ))
    }

    // リードが一番大きくなるような手を見つける
    val nowBoard = megurimasu.conversion()
    var maxScore = -99
    val bestBehavior = nextBehaviors
            .asSequence()
            .maxBy { it ->
                megurimasu.action(it)
                val (score, _) = searchBestBehavior(megurimasu, depth - 1, probability)
                megurimasu.deconversion(nowBoard)

                maxScore = max(score, maxScore)
                score
            }!!

    return Pair(maxScore, mapOf("A_1" to bestBehavior["A_1"]!!, "A_2" to bestBehavior["A_2"]!!))
}

fun strategyOfBruteForce(megurimasu: MegurimasuSimulator, agentName: String, num: Int): List<Int>{
    val actionedScoreList = arrayListOf<Int>()
    for(i in 0..7){
        val movableList = listOf(0, 1, 2, 3, 4, 5, 6, 7).filter { it -> it != (i+4)%8 }

        // 現在の盤面から1つ手を選択した時，それに対して新たに手を選択した合計2手のスコアを計算して集計する
        // 必要なのは1手後の情報だけなので，2手後の選択については特に選択した手の保持などをしない
        var maxValue = -99
        movableList.forEach{ type ->
            val agentX = megurimasu.agents[agentName]!!.x
            val agentY = megurimasu.agents[agentName]!!.y
            val (actionX, actionY) = getActionPos(agentX, agentY, i)
            val (actionXTwo, actionYTwo) = getActionPos(actionX, actionY, type)

            try {
                maxValue = max(megurimasu.scoreData[actionY][actionX] + megurimasu.scoreData[actionYTwo][actionXTwo], maxValue)
            } catch (e: IndexOutOfBoundsException) {
                // 要素外参照エラー
                // このエラーが起きた時は集計しない
            }
        }

        actionedScoreList.add(maxValue)
    }

    // スコアを降順にソートして指定数だけ選択してそのidxを返す
    return actionedScoreList
            .toIntArray()
            .mapIndexed{ idx, elem -> idx to elem }
            .sortedByDescending { ( _, value) -> value }
            .take(num)
            .map { it.first }
}

fun strategyOfStalker(megurimasu: MegurimasuSimulator, agentName: String, num: Int): List<Int>{
    // 存在しないエージェントの名前が引数で与えられたとき時は全てが8のListを返す
    if(agentName !in megurimasu.agents.keys){
        return Array(num){ _ -> 8}.toList()
    }

    // 一番近い敵エージェントを探す
    val enemyTeam = if("A" in agentName) "B" else "A"
    val agent = megurimasu.agents[agentName]!!
    val enemyAgents = arrayOf(megurimasu.agents["${enemyTeam}_1"]!!, megurimasu.agents["${enemyTeam}_2"]!!)
    val minDistAgent = enemyAgents
            .minBy { calDist(agent.x, agent.y, it.x, it.y) }!!

    // 一番近いエージェントに近づくための行動タイプを探す
    val meAgentDegree = calDegree2Points(agent.x, agent.y, minDistAgent.x, minDistAgent.y).toInt()
    val optimalActionType = (meAgentDegree % 360 / 45 + 2) % 8

    // 評価の高いものから順にListに放り込む
    val retList = mutableListOf(optimalActionType)
    for(i: Int in 1..4){
        retList.add((optimalActionType + i + 8) % 8)
        retList.add((optimalActionType + (i * -1) + 8) % 8)
    }

    return retList.take(num)
}

fun strategyOfPrayToGod(megurimasu: MegurimasuSimulator, agentName: String, num: Int): List<Int>{
    // ランダムに値を選択してListに詰めて返す
    val retList = mutableListOf<Int>()
    for(i in 0 until num){
        var randValue = 0
        do{
            randValue = random.nextInt(8) + random.nextInt(2) * 10
        }while(retList.contains(randValue))

        retList.add(randValue)
    }

    return retList
}