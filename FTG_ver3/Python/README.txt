To run a game from Python, please read the following instructions. We assume that you have successfully installed Py4J and FightingICE ver 3.00.

1. Start FightingICE with arguments of your choice as follows:
"--py4j" : Python mode => If this argument is specified, a message "Waiting python to launch a game" will be shown in the game screen. You can then run a game or multiple games, each with different port number (see below), using a launching Python script. You can find sample launching scripts Main~.py and sample Python AIs in this folder.
"--port [portNumber]": This is for setting the port number when you use Python. The default port number is 4242.
"--limithp [P1HP] [P2HP]" : Limit-HP Mode => Launch FightingICE with the HP mode used in the competition for both Standard and Speedrunning Leagues, where P1HP and P2HP are the initial HPs of P1 and P2, respectively.
"--black-bg": Black-background mode => This modes runs FightingICE with a black background (without a background image), recommended when you use a visual-based AI, controlled by deep neural networks, etc. In the competition, all games will be run in this mode.
"--inverted-player [playerNumber]": Inverted-color mode => playerNumber is 1 and 2 for P1 and P2, respectively; if playerNumber is a number besides 1 or 2 (e.g., 0), the original character colors are used. This mode internally uses -- the colors shown on the game screen are not affected -- the inverted colors for a specified character and is recommended when you use getDisplayByteBufferAsBytes. This mode enables both characters to be distinguishable by their color differences even though they are the same character type, which should be helpful when you use a visual-based AI, controlled by deep neural networks, etc. In the competition, all games will also be run in "--inverted-player 1".

2. Execute a launching script Main~.py
    e.g.) python Main_JavaAIvsJavaAI_MctsAivsMachete_4242_default_port.py -n 3 &
In this, case, you are able to do three games. For Java-based AIs like MctsAi and Machete in this example, we assume that you already have them in \data\ai,
where FightingICE has been installed.

3. If you want to run multiple game processes simultaneously on a same PC (in case you have a high spec PC since running multiple game processes might cause the FPS rate to drastically drop on low performance PCs) through a launching script Main~.py, please first run each FightingICE process with
--py4j --port PORT_NUMBER
For example,
--py4j --port 4000
, where the default port number is 4242; if no port number is explicitly specified here, port 4242 will be used.
And then please execute the launching Main~.py that has the line specifying the gateway server's port to the same port of the corresponding FightingICE process:
gateway = JavaGateway(gateway_parameters=GatewayParameters(port=PORT_NUMBER), callback_server_parameters=CallbackServerParameters(port=0))
For example, in response to the previous example,
gateway = JavaGateway(gateway_parameters=GatewayParameters(port=4000), callback_server_parameters=CallbackServerParameters(port=0))
Note that in the launching script, even though the default port is used, 4242 must be explicitly specified for PORT_NUMBER.

4. As shown in sample launching scripts Main~.py, you can run a fight or fights of
a Java AI vs a Python AI (a Python AI vs a Java AI),
a Java AI vs a Java AI, or
a Python AI vs a Python AI
from Python.

5. Python AIs just use the same interface as the Java one (AIInterface), and you can create a basic Python AI as shown in our samples: KickAi.py, machete.py (our Python implementation of the 2015 competition winner), and DisplayInfo.py (a simple visual-based AI). Once you have a Python AI, registration of it to the manager in the launching script Main~.py can be done as follows:
#First, import a Python AI; for example
from KickAI import KickAI

#Then register the imported Python AI to the manager
manager.registerAI("KickAI", KickAI(gateway))

Hope this helps.
Team FightingICE
---------------------------------------------------