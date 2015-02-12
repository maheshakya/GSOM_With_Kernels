package com.gsom.core;

import com.gsom.enums.DeletionType;
import com.gsom.enums.InitType;
import com.gsom.objects.GNode;
import com.gsom.util.Data;
import com.gsom.util.GSOMConstants;
import com.gsom.util.Utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GSOMTrainer {

    private Map<String, GNode> nodeMap;
    private NodeGrowthHandler growthHandler;
    private InitType initType;
    private DeleteBase D = null;

    public GSOMTrainer(InitType initType, DeletionType deleteType) {
        this.initType = initType;
        nodeMap = new HashMap<String, GNode>();
        growthHandler = new NodeGrowthHandler();

        if (deleteType == DeletionType.FBR) {
            D = new FBR();
        } else if (deleteType == DeletionType.ARC) {
            D = new ARC();
        }
    }

    public Map<String, GNode> trainNetwork(ArrayList<String> iStrings, ArrayList<double[]> iWeights) {
//        for(int i=0; i<iWeights.size();i++)
//            iWeights.set(i, Utils.normalizeVectorMinMax(iWeights.get(i)));
        if (initType == InitType.PCA) {
            initFourNodes(iWeights);
        } else {
            initFourNodes(initType);	//init the map with four nodes
        }
        for (int i = 0; i < GSOMConstants.MAX_ITERATIONS; i++) {
            int k = 0;
            double learningRate = Utils.getLearningRate(i, nodeMap.size());
            double radius = Utils.getRadius(i, Utils.getTimeConst());
            for (double[] input : iWeights) {
                trainForSingleIterAndSingleInput(i, input, iStrings.get(k), learningRate, radius);
                k++;
            }
        }
        return nodeMap;
    }

    private void trainForSingleIterAndSingleInput(int iter, double[] input, String str, double learningRate, double radius) {

        GNode winner = Utils.selectWinner(nodeMap, input);

        winner.calcAndUpdateErr(input);

        for (Map.Entry<String, GNode> entry : nodeMap.entrySet()) {
            entry.setValue(Utils.adjustNeighbourWeight(entry.getValue(), winner, input, radius, learningRate));
        }

        if (winner.getErrorValue() > GSOMConstants.getGT()) {
            //System.out.println("Winner "+winner.getX()+","+winner.getY()+" GT exceeded");
            adjustWinnerError(winner);
        }

        if (D != null) {
            D.update(Utils.generateIndexString(winner.getX(), winner.getY()));
        }
    }

    //Initialization of the map.
    //this will create 4 nodes with random weights
    private void initFourNodes(InitType type) {
        if (type == InitType.RANDOM) {
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    GNode initNode = new GNode(i, j, Utils.generateRandomArray(GSOMConstants.DIMENSIONS));
                    nodeMap.put(Utils.generateIndexString(i, j), initNode);
                    if (D != null) {
                        D.update(Utils.generateIndexString(i, j));
                    }
                }
            }
        } else if (type == InitType.LINEAR) {
            double initVal = 0.1;
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 2; j++) {
                    GNode initNode = new GNode(i, j, Utils.generateLinearArray(GSOMConstants.DIMENSIONS, initVal));
                    nodeMap.put(Utils.generateIndexString(i, j), initNode);
                    initVal += 0.1;

                    if (D != null) {
                        D.update(Utils.generateIndexString(i, j));
                    }
                }
            }
        }
    }

    //when a neuron wins its error value needs to be adjusted
    private void adjustWinnerError(GNode winner) {

        //on x-axis
        String nodeLeftStr = Utils.generateIndexString(winner.getX() - 1, winner.getY());
        String nodeRightStr = Utils.generateIndexString(winner.getX() + 1, winner.getY());

        //on y-axis
        String nodeTopStr = Utils.generateIndexString(winner.getX(), winner.getY() + 1);
        String nodeBottomStr = Utils.generateIndexString(winner.getX(), winner.getY());

        if (nodeMap.containsKey(nodeLeftStr)
                && nodeMap.containsKey(nodeRightStr)
                && nodeMap.containsKey(nodeTopStr)
                && nodeMap.containsKey(nodeBottomStr)) {
            distrErrToNeighbors(winner, nodeLeftStr, nodeRightStr, nodeTopStr, nodeBottomStr);
        } else {
            growthHandler.growNodes(nodeMap, winner); //NodeGrowthHandler takes over
        }
    }

    //distributing error to the neighbors of thw winning node
    private void distrErrToNeighbors(GNode winner, String leftK, String rightK, String topK, String bottomK) {
        winner.setErrorValue(GSOMConstants.getGT() / 2);
        nodeMap.get(leftK).setErrorValue(calcErrForNeighbour(nodeMap.get(leftK)));
        nodeMap.get(rightK).setErrorValue(calcErrForNeighbour(nodeMap.get(rightK)));
        nodeMap.get(topK).setErrorValue(calcErrForNeighbour(nodeMap.get(topK)));
        nodeMap.get(bottomK).setErrorValue(calcErrForNeighbour(nodeMap.get(bottomK)));
    }

    //error calculating equation for neighbours of a winner
    private double calcErrForNeighbour(GNode node) {
        return node.getErrorValue() + (GSOMConstants.FD * node.getErrorValue());
    }

    private void initFourNodes(ArrayList<double[]> iweights) {
        double[][] input = new double[iweights.size()][iweights.get(0).length];
        for (int i = 0; i < iweights.size(); i++) {
            for (int j = 0; j < iweights.get(0).length; j++) {
                input[i][j] = iweights.get(i)[j];
            }
        }

        double[][] scores = Data.PCANIPALS(input, 4);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                GNode initNode = new GNode(i, j, scores[2 * i + j]);
                nodeMap.put(Utils.generateIndexString(i, j), initNode);
                if (D != null) {
                    D.update(Utils.generateIndexString(i, j));
                }
            }
        }
    }
}
