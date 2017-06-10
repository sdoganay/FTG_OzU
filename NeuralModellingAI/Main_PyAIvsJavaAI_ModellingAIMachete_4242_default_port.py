import sys
from time import sleep
from py4j.java_gateway import JavaGateway, GatewayParameters, CallbackServerParameters, get_field
from NeuralModellingAI import NeuralModellingAI


def check_args(args):
	for i in range(argc):
		if args[i] == "-n" or args[i] == "--n" or args[i] == "--number":
			global GAME_NUM
			GAME_NUM = int(args[i+1])

def start_game():
	for i in range(GAME_NUM):
		manager.registerAI("NeuralModellingAI", NeuralModellingAI(gateway))
		print("Start game", i)
	
		game = manager.createGame("ZEN", "ZEN", "NeuralModellingAI", "Machete")
		manager.runGame(game)
	
		print("After game", i)
		sys.stdout.flush()

def close_gateway():
	gateway.close_callback_server()
	gateway.close()
	
def main_process():
	check_args(args)
	start_game()
	close_gateway()

args = sys.argv
argc = len(args)
GAME_NUM = 1
gateway = JavaGateway(gateway_parameters=GatewayParameters(port=4242), callback_server_parameters=CallbackServerParameters(port=0))
python_port = gateway.get_callback_server().get_listening_port()
gateway.java_gateway_server.resetCallbackClient(gateway.java_gateway_server.getCallbackClient().getAddress(), python_port)
manager = gateway.entry_point

main_process()


