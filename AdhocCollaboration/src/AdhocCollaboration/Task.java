/**
 * 
 */
package AdhocCollaboration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;

import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;

/**
 * @author Xi Chen
 * @author Bin Chen
 *
 */
public class Task {
    private int Id;
	private int type;
	private int numSubtasks;
	private Parameters params;
	//private int numTasksFinished;
//	private double taskOpenness;
//	private double taskProb;
//	private double globalSat;
	private boolean isFinished;
	private ArrayList<SubTask> subtasksList;
	private Random random;
	private int tickToFinish;
	private double reward=1;//the reward for completing this task, now we just set it to be 1 for every task
	private int NumAgentsRequired;//numAgents required for all subtasks combined
    
    public Task() {
		Id 				= 0;
		type			= 0;
		numSubtasks 	= 0;
		params 			= RunEnvironment.getInstance().getParameters();
		//numTasksFinished= 0;
//		taskOpenness 	= 0.0;
//		taskProb 		= 0.0;
//		globalSat 		= 0.0;
		isFinished 		= false;
		subtasksList 	= new ArrayList<SubTask>();
		random 			= new Random();
		tickToFinish = (Integer) params.getValue ("tickToFinish");;//change ticks to finish a task, only change it here
		reward=1 + (int)(Math.random() * ((100 - 1) + 1));//get a random number between 1 to 100;
				
				/*
				 * Notice for the use of Math.random
				 *reference: http://stackoverflow.com/questions/363681/generating-random-integers-in-a-range-with-java
				 *
				 * Min + (Math.random() * (Max - Min))
				 *	You now will get a value in the range [Min,Max). example, if Max=10, Min=5, then Following our example, that means [5,10)
				 *	Min + (int)(Math.random() * ((Max - Min) + 1))
				 * this way we get [5,10]
				 */
	}
    
    
    
    /**
     * this method calculates the required agents number by adding the subtasks required agents
     */
    public int getNumReqiredAgents(){
    	return this.NumAgentsRequired;
    	
    }
    
    
    public int getNumAssigedAgents(){
    	int total=0;
    	for(SubTask subtask:this.subtasksList){
    		total+=subtask.getSelectedNumAgentsCount();
    	}
    	return total;
    }
    
    
    public void calculateNumAgentsRequired(){
    	int total=0;
    	for(SubTask subtask:this.subtasksList){
    		total+=subtask.getNumAgents();
    	}
    	this.setNumAgentsRequired(total);
    	
    }
    
    
    
    /**
     * 
     */
    public int getTickToFinish(){
    	return tickToFinish;
    }
    
    /**
    *
    *@param id 	Task Id
    **/
    public void setId(int id){
    	Id = id;
    }

    /**
    *
    *@return id Task Id
    **/
    public int getId(){
    	return Id;
    }
    
    /**
    *
    *@return An array of subtask contained in this task
    **/
    public ArrayList<SubTask> getSubtasks(){
    	return subtasksList;
    }
    
    /**
     * 
     * @param num
     */
    public void setNumSubtasks(int num){
    	numSubtasks = num;
    }
    
    /**
     * 
     * @return
     */
    public int getNumSubtasks(){
    	return numSubtasks;
    }
    
    /**
     * 
     * @param taskType
     */
    public void setType(int taskType){
    	type = taskType;
    }
    
    /**
     * 
     * @return
     */
    public int getType(){
    	return type;
    }
    
    /**
     * 
     * @param openness
     */
//    public void setTaskOpenness(double openness){
//    	taskOpenness = openness;
//    }
//    
//    /**
//     * 
//     * @return
//     */
//    public double getTaskOpenness(){
//    	return taskOpenness;
//    }
    
//    /**
//     * 
//     */
//    public void setFinished(){
//    	isFinished = true;
//    	numTasksFinished++; //why?
//    }
    
    /**
     * 
     */
    public boolean getFinished(){
    	return isFinished;
    }
    
    
    /**
     * 
     * @param prob
     */
//    public void setTaskProb(double prob){
//		taskProb = prob;
//	}
//    
//    /**
//     * 
//     * @return
//     */
//	public double getTaskProb() {
//		return taskProb;
//	}
	
//	/**
//	 * 
//	 * @return
//	 */
//	public int getNumTasksFinished() {
//		return numTasksFinished;
//	}

	/**
	* decompose a task into subtasks,
	* it creates subtasks and put them into subtasksList of this task,  we did not use the returned value for now.
	*@param subtasks 	A List of subtasks'Id
	*@return subtaskList 	A list of subtasks
	* @throws IOException 
	*/
	public ArrayList<SubTask> decompose(ArrayList<Integer> subtasks,ArrayList<Integer> Num_AgentsList, ArrayList<Double> QualityList){
		for (int i=0; i<subtasks.size(); i++) {
			SubTask st = new SubTask();
			st.setId(subtasks.get(i));
			st.setCap(subtasks.get(i));
			st.setNumAgents(Num_AgentsList.get(i));
			st.setQuality(QualityList.get(i));
			subtasksList.add(st);
		}
		this.calculateNumAgentsRequired();
		return subtasksList;
	}
	
	
	public String getTaskDetial(){
		StringBuilder sb= new StringBuilder();
		sb.append(String.format("%d,%d,%d,",this.Id,this.type,MainAgent.tick));
		for (SubTask s : this.subtasksList){
			sb.append(s.getId()+" ");
			sb.append(s.getNumAgents()+" ");
			sb.append(s.getQuality()+",");
		}
		return sb.toString();
	}
	
	
//	/**
//	 * 
//	 * @return
//	 */
//	public double getGlobalSat(){
//		return globalSat;
//	}
	
//	/**
//	 * 
//	 */
//	public void setGlobalSat(){
//		double localValue = 0;
//		ArrayList<SubTask> subtasks = this.getSubtasks();
//		for(int i = 0; i < subtasks.size(); i++){
//			localValue += subtasks.get(i).getLocalSatAllAgents();
//		}
//		if(isFinished){
//			globalSat = localValue + random.nextGaussian();
//		}
//	}
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}


	public double getReward() {
		return reward;
	}


	public void setReward(double reward) {
		this.reward = reward;
	}



	public int getNumAgentsRequired() {
		return NumAgentsRequired;
	}



	public void setNumAgentsRequired(int numAgentsRequired) {
		NumAgentsRequired = numAgentsRequired;
	}

}
