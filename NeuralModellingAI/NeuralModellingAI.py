from py4j.java_gateway import get_field, JavaGateway
from abc import abstractmethod
import tensorflow as tf
import config,pickle, agent_modelling,time
from sklearn.preprocessing import MultiLabelBinarizer
import numpy

ThunderPCA, NilmiriPCA, RaneziPCA = 0,0,0
mlb = MultiLabelBinarizer()
ThunderPrediction = lambda *args: None
NilmiriPrediction = lambda *args: None
RaneziPrediction = lambda *args: None


class RunAsFastAsYouCan(Exception):
    pass

class FightingController:

    # ---- Do NOT override these methods ----

    class Java:
        implements = ["gameInterface.AIInterface"]

    def __init__(self, gateway):
        self.gateway = gateway # type: JavaGateway

        # Good practice to declare instance variables in __init__
        self.frameData = None # Updated after every call to getInformation
        self.inputKey = None
        self.cc = None
        self.player = None
        self.gameData = None
        self.simulator = None
        self.isGameJustStarted = True

    def getInformation(self, frameData):
        """
        Receives and stores frame data for use by other methods.
        :param frameData: data of the current frame.
        """
        self.frameData = frameData

    def initialize(self, gameData, player):
        """
        Prepares this AI for operation.
        :param gameData: information about the game the AI is about to enter
        :param player: boolean flag indicating whether this AI controls P1 or P2
        :return: 0 if the initialization is successful, the error code otherwise
        """
        self.inputKey = self.gateway.jvm.structs.Key()
        self.frameData = self.gateway.jvm.structs.FrameData()
        self.cc = self.gateway.jvm.commandcenter.CommandCenter()
            
        self.player = player
        self.gameData = gameData
        self.simulator = self.gameData.getSimulator()

        self.init()

        return 0

    def input(self):
        """
        Returns the command decided by the AI for the current frame.
        :return: the command decided by the AI for the current frame.
        """
        return self.inputKey


    def processing(self):
        """
        Delivers the input for the current frame. If no command is set, this
        :return:
        """
        if not self.frameData.getEmptyFlag() and self.frameData.getRemainingTime() > 0:

            # Let CommandCenter check if this is a new round - if so, the current skill should be canceled.
            self.cc.setFrameData(self.frameData, self.player)

            if not self.cc.getskillFlag():
                self.inputKey.empty()
                self.cc.skillCancel()
                command = self.computeCommand()
                if command is not None:
                    self.cc.commandCall(command)

            self.inputKey = self.cc.getSkillKey()


    # ---- DO override these methods ----

    def getPCAByName(self,agentName):
        pca_address = agentName + 'pca_dumped.pkl'
        with open(pca_address, 'rb') as fid:
            pca_loaded = pickle.load(fid)
            return pca_loaded


    def initThunderSession(self):
        states = tf.placeholder(tf.float32, name="states")
        logits = agent_modelling.inference(states,config.THUNDER_NAME)
        predOp = tf.argmax(logits, 1)

        # variable_averages = tf.train.ExponentialMovingAverage(
        #     agent_modelling.MOVING_AVERAGE_DECAY)
        # variables_to_restore = variable_averages.variables_to_restore()
        variables_to_restore=tf.global_variables()
        # print("variables_to_restore THUNDER",variables_to_restore)
        saver = tf.train.Saver([k for k in variables_to_restore if k.name.startswith(config.THUNDER_NAME)])
        sess = tf.Session()
        ckpt = tf.train.get_checkpoint_state(config.getCheckpointDir(config.THUNDER_NAME))
        if ckpt and ckpt.model_checkpoint_path:
            print("checkpoint for Thunder",ckpt.model_checkpoint_path)
            saver.restore(sess, ckpt.model_checkpoint_path)
        else:
            print('No checkpoint file found')
            return

        global ThunderPrediction
        ThunderPrediction = lambda x: agent_modelling.label_index_to_motion(sess.run(predOp, feed_dict={states: x})[0])

    def initNilmiriSession(self):
        states = tf.placeholder(tf.float32, name="states")
        logits = agent_modelling.inference(states,config.NILMIRI_NAME)
        predOp = tf.argmax(logits, 1)

        # variable_averages = tf.train.ExponentialMovingAverage(
        #     agent_modelling.MOVING_AVERAGE_DECAY)
        # variables_to_restore = variable_averages.variables_to_restore()

        variables_to_restore=tf.global_variables()
        # print("variables_to_restore NILMIRI",variables_to_restore)
        saver = tf.train.Saver([k for k in variables_to_restore if k.name.startswith(config.NILMIRI_NAME)])
        sess = tf.Session()
        ckpt = tf.train.get_checkpoint_state(config.getCheckpointDir(config.NILMIRI_NAME))
        if ckpt and ckpt.model_checkpoint_path:
            print("checkpoint for Nilmiri",ckpt.model_checkpoint_path)
            saver.restore(sess, ckpt.model_checkpoint_path)
        else:
            print('No checkpoint file found')
            return

        global NilmiriPrediction
        NilmiriPrediction = lambda x: agent_modelling.label_index_to_motion(sess.run(predOp, feed_dict={states: x})[0])

    def initRaneziSession(self):
        states = tf.placeholder(tf.float32, name="states")
        logits = agent_modelling.inference(states,config.RANEZI_NAME)
        predOp = tf.argmax(logits, 1)

        # variable_averages = tf.train.ExponentialMovingAverage(
        #     agent_modelling.MOVING_AVERAGE_DECAY)
        # variables_to_restore = variable_averages.variables_to_restore()

        variables_to_restore=tf.global_variables()
        # print("variables_to_restore RANEZI",variables_to_restore)
        saver = tf.train.Saver([k for k in variables_to_restore if k.name.startswith(config.RANEZI_NAME)])

        sess = tf.Session()
        ckpt = tf.train.get_checkpoint_state(config.getCheckpointDir(config.RANEZI_NAME))
        if ckpt and ckpt.model_checkpoint_path:
            print("checkpoint for Ranezi",ckpt.model_checkpoint_path)
            saver.restore(sess, ckpt.model_checkpoint_path)
        else:
            print('No checkpoint file found')
            return

        global RaneziPrediction
        RaneziPrediction = lambda x: agent_modelling.label_index_to_motion(sess.run(predOp, feed_dict={states: x})[0])

    def init(self):
        """
        Prepares this AI for operation.
        """
        time11 = time.time()
        try:
            print("initializing the AI.")
            global ThunderPCA, NilmiriPCA, RaneziPCA
            ThunderPCA = self.getPCAByName(config.THUNDER_NAME)
            NilmiriPCA = self.getPCAByName(config.NILMIRI_NAME)
            RaneziPCA = self.getPCAByName(config.RANEZI_NAME)

            global mlb
            motions = agent_modelling.initMotions()
            # print("# of motions",motions.shape)
            mlb = MultiLabelBinarizer(motions)

            self.initThunderSession()
            self.initNilmiriSession()
            self.initRaneziSession()

            print("init finished!")
        except Exception as e:
            from traceback import print_exc
            print_exc()

        print("elapsed ", (time.time() - time11) * 1000, " ms for initilization")

    @abstractmethod
    def getCharacter(self):
        """
        Returns the character to be controlled by this AI.
        Legal values are 'ZEN', 'GARNET', 'LUD', and 'KFM'.
        :return: the character to be controlled by this AI.
        """
        return "ZEN"


    @abstractmethod
    def computeCommand(self):
        """
        Computes the next command to execute. This method is only called after the previous command is done executing.
        :return: the next command to execute.
        """
        pass


    def close(self):
        """
        Executes operations necessary before the AI is shut down (e.g., saving data etc.).
        """
        ##TODO: close all sessions

class NeuralModellingAI(FightingController):


    def getCharacter (self):
        return 'ZEN'

    def onehot(self, data):
        # data = ['AIR']
        global mlb
        # inception = numpy.array(list(map(lambda n: [n], data)))
        inception = numpy.array([data])
        encoded = mlb.fit_transform(inception)
        return encoded

    def getStateFromFrame(self):
        my = self.cc.getMyCharacter();
        myAction = get_field(my, "action").__str__()
        if ("RECOV" in myAction or "THROW" in myAction):
            # run away
            print("running away.")
            raise RunAsFastAsYouCan

        opp = self.cc.getEnemyCharacter();
        state = [[get_field(my, "x"), get_field(my, "y"), get_field(my, "energy"), get_field(my, "hp"),
                 get_field(opp, "x"), get_field(opp, "y"), get_field(opp, "energy"), get_field(opp, "hp")]]

        oppAction = get_field(opp, "action").__str__()

        onehotted = self.onehot([oppAction])
        state = numpy.concatenate((state, onehotted),axis=1)

        return state


    def askFromThunder(self,state):
        try:
            global ThunderPCA,ThunderPrediction
            # print("state shape:",state.shape)
            state_pca = ThunderPCA.transform(state)
            y_pred = ThunderPrediction(state_pca)
            return y_pred
        except Exception:
            from traceback import print_exc
            print_exc()

    def askFromNilmiri(self,state):
        try:
            global NilmiriPCA,NilmiriPrediction
            state_pca = NilmiriPCA.transform(state)
            y_pred = NilmiriPrediction(state_pca)
            return y_pred
        except Exception:
            from traceback import print_exc
            print_exc()

    def askFromRanezi(self,state):
        try:
            global RaneziPCA,RaneziPrediction
            state_pca = RaneziPCA.transform(state)
            y_pred = RaneziPrediction(state_pca)
            return y_pred
        except Exception:
            from traceback import print_exc
            print_exc()

    def voteFor(self,votes):
        print("votes:",votes)
        uniques, counts = numpy.unique(votes, return_counts=True)
        return uniques[counts.argmax()]

    def computeCommand (self):
        try:
            time1 = time.time()
            state = self.getStateFromFrame()
            print("elapsed ",(time.time()-time1)*1000," ms for state generation")

            time2 = time.time()
            ThunderVote = self.askFromThunder(state)
            print("elapsed ",(time.time()-time2)*1000," ms for Thunder's action prediction")

            time2 = time.time()
            NilmiriVote = self.askFromNilmiri(state)
            print("elapsed ",(time.time()-time2)*1000," ms for Nilmiri's action prediction")


            time2 = time.time()
            RaneziVote = self.askFromRanezi(state)
            print("elapsed ",(time.time()-time2)*1000," ms for Ranezi's action prediction")

            action = self.voteFor([ThunderVote, NilmiriVote, RaneziVote])
            # action = ThunderVote
            print("action taken:",action)
            print("elapsed ",(time.time()-time1)*1000," ms for total")
            return action

        except RunAsFastAsYouCan:
            print("elapsed ",(time.time()-time1)*1000," ms for total")
            return "JUMP"
        except Exception:
            from traceback import print_exc
            print_exc()


