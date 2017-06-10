from py4j.java_gateway import get_field

class ModellingAI(object):
    def __init__(self, gateway):
        self.gateway = gateway
        
    def getCharacter(self):
        return "ZEN"
        
    def close(self):
        pass
        
    def getInformation(self, frameData):
        # Getting the frame data of the current frame
        self.frameData = frameData
        
    def initialize(self, gameData, player):
        # Initializng the command center, the simulator and some other things
        self.inputKey = self.gateway.jvm.structs.Key()
        self.frameData = self.gateway.jvm.structs.FrameData()
        self.cc = self.gateway.jvm.commandcenter.CommandCenter()
            
        self.player = player
        self.gameData = gameData
        self.simulator = self.gameData.getSimulator()
                
        return 0
        
    def input(self):
        # Return the input for the current frame
        return self.inputKey
        
    def processing(self):
        # Just compute the input for the current frame
        if self.frameData.getEmptyFlag() or self.frameData.getRemainingTime() <= 0:
                self.isGameJustStarted = True
                return
                
        self.cc.setFrameData(self.frameData, self.player)
                
        if self.cc.getskillFlag():
                self.inputKey = self.cc.getSkillKey()
                return
                
        # Just spam kick
        self.cc.commandCall("B")
                        
    # This part is mandatory
    class Java:
        implements = ["gameInterface.AIInterface"]