# ユーティリティ
汎用的な関数や変数を置く

## 関数
- getAgentPosFromQR(): Map<String, Array<Int>>
	- QRデータからエージェントの初期位置を読み込んで返す

- util.getTeamID(agentName: String): Int
	- エージェントIDに対応したチームIDを返す
	- A -> 1, B -> 2

- calDict(x: Int, y: Int, x_1: Int, y_1: Int): Double
	- (x, y)と(x_1, y_1)の2点間の距離を計算して返す

- util.calDegree2Points(x: Double, y: Double, x_2: Double, y_2: Double): Double
	- (x, y)と(x_1, y_1)の2点の角度(度数法)を計算して返す

## 変数
- movementValue: Map<Integer, Map<String, Int>>
	- エージェントをある方向へ移動させるための移動量を持つ
