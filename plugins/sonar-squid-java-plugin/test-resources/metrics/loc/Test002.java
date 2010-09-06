import InputNeuron;
import java.util.*;

public interface Neuron {

    public FloatPoint   getWeight();
    public void         setWeight(FloatPoint fpWeight);
    
    public float        distanceTo(Neuron nOtherNeuron);
 
    public void         moveTo(InputNeuron inGoalNeuron, int netDistance);
    /*schaufelt alle neuen Gewichte zu den alten um*/
    public void         move();
   
    public void         addNeighbor(Neuron neuron);
    public void         addMetaNeighbor(Neuron neuron);
    public Enumeration  getNeighbors();
    public Enumeration  getMetaNeighbors();
}