import sys,os,time
from time import sleep
# from py4j.java_gateway import JavaGateway, GatewayParameters, CallbackServerParameters, get_field

def check_args(args):
    for i in range(argc):
        if args[i] == "-n" or args[i] == "--n" or args[i] == "--number":
            global GAME_NUM
            GAME_NUM = int(args[i+1])

def start_game():
    # source = "path-to-source/FTG3.10/data/ai"
    # agents = list(map(lambda f: f[:-4], filter(lambda filename: filename.endswith(".jar"), os.listdir(source))))
    # for agent1 in agents:
    #     for agent2 in agents:
    #         for i in range(GAME_NUM):
    #             print("Starting the game {0} between A1={1} and A2={2}".format(i+1,agent1,agent2))
    #             game = manager.createGame("ZEN", "ZEN", agent1, agent2)
    #             manager.runGame(game)
    #             print("Game finished")
    #             print ("-"*20)
    # sys.stdout.flush()
    # print ("-"*20)
    print("game_num",GAME_NUM)
    print("all games finished")

def close_gateway():
    # gateway.close_callback_server()
    # gateway.close()
    print("Hi!")

def main_process():
    check_args(args)
    start_game()
    close_gateway()

args = sys.argv
argc = len(args)
GAME_NUM = 1
# gateway = JavaGateway(gateway_parameters=GatewayParameters(port=4242), callback_server_parameters=CallbackServerParameters(port=0))
# python_port = gateway.get_callback_server().get_listening_port()
# gateway.java_gateway_server.resetCallbackClient(gateway.java_gateway_server.getCallbackClient().getAddress(), python_port)
# manager = gateway.entry_point

main_process()
