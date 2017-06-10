
"""Builds the network.

Summary of available functions:

 # Compute input states and actions for training. If you would like to run
 # evaluations, use inputs() instead.
 inputs, actions = distorted_inputs()

 # Compute inference on the model inputs to make a prediction.
 predictions = inference(inputs)

 # Compute the total loss of the prediction with respect to the actions.
 loss = loss(predictions, actions)

 # Create a graph to run one step of training with respect to the loss.
 train_op = train(loss, global_step)
"""
# pylint: disable=missing-docstring
# from __future__ import absolute_import
# from __future__ import division
# from __future__ import print_function

import re,numpy, pandas,config
import tensorflow as tf

state_column_size = 64
number_of_categories = 46
n_components = 16

motions = numpy.array([])

MOVING_AVERAGE_DECAY = 0.9999     # The decay to use for the moving average.

TOWER_NAME = 'tower'
#
# def _activation_summary(x):
#   tensor_name = re.sub('%s_[0-9]*/' % TOWER_NAME, '', x.op.name)
#   tf.summary.histogram(tensor_name + '/activations', x)
#   tf.summary.scalar(tensor_name + '/sparsity',
#                                        tf.nn.zero_fraction(x))

def _variable_on_cpu(name, shape, initializer):
  with tf.device('/cpu:0'):
    var = tf.get_variable(name, shape, initializer=initializer)
  return var

def _variable_with_weight_decay(name, shape, stddev, wd):
  var = _variable_on_cpu(
      name,
      shape,
      tf.truncated_normal_initializer(stddev=stddev))
  if wd is not None:
    #L2 REGULARIZATION IS INSERTED!
    weight_decay = tf.multiply(tf.nn.l2_loss(var), wd, name='weight_loss')
    tf.add_to_collection('losses', weight_decay)
  return var

def inference(states,agentName):
    global NUM_EXAMPLES_PER_EPOCH_FOR_TRAIN
    NUM_EXAMPLES_PER_EPOCH_FOR_TRAIN = states.get_shape()[0].value

    global n_components

    # layer1
    with tf.variable_scope(agentName + 'layer1') as scope:
        weights = _variable_with_weight_decay(agentName + 'weights', shape=[n_components, int(n_components)],
                                              stddev=0.04, wd=0.001)
        biases = _variable_on_cpu(agentName + 'biases', [int(n_components)], tf.constant_initializer(0.1))
        layer1 = tf.nn.relu(tf.matmul(states, weights) + biases, name=scope.name)

    # layer2
    with tf.variable_scope(agentName + 'layer2') as scope:
        weights = _variable_with_weight_decay(agentName + 'weights', shape=[int(n_components), number_of_categories],
                                              stddev=0.04, wd=0.001)
        biases = _variable_on_cpu(agentName + 'biases', [number_of_categories], tf.constant_initializer(0.1))
        layer2 = tf.nn.sigmoid(tf.matmul(layer1, weights) + biases, name=scope.name)

    # linear layer(WX + b),
    # We don't apply softmax here because
    # tf.nn.sparse_softmax_cross_entropy_with_logits accepts the unscaled logits
    # and performs the softmax internally for efficiency.
    with tf.variable_scope(agentName + 'softmax_linear') as scope:
        weights = _variable_with_weight_decay(agentName + 'weights', [number_of_categories, number_of_categories],
                                              stddev=1 / 56, wd=0.0)
        biases = _variable_on_cpu(agentName + 'biases', [number_of_categories],
                                  tf.constant_initializer(0.0))
        softmax_linear = tf.add(tf.matmul(layer2, weights), biases, name=scope.name)

    return softmax_linear


def initMotions():
    global motions
    dataframe = pandas.read_csv(config.CHARACTER_MOTION_PATH, header=None)
    motions = dataframe.values[1:, 0]
    # motions = numpy.array([mo for mo in motions if "RECOV" not in mo and "THROW" not in mo])
    return motions

# def initLabelBinarizer():
#
#     mlb = MultiLabelBinarizer(motions)
#     inception = numpy.array(list(map(lambda n: [n], data)))

def label_index_to_motion(i):
    global motions
    return motions[i]

# def label_index_to_motion(motions,i):
#     return motions[i]
