/**
 * 
 */
package AdhocCollaboration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 * @author Xi Chen
 * @author Bin Chen
 *
 */
public class BlackboardMessage {
	private Task task;
	private ArrayList <SubTask> subtasks;
	private HashMap<Integer, ArrayList<Integer>> subtaskAssignment; // Subtask assignment: subtaskId-->[ agentId,...]. Assign each subtask to a list of agents.
	private ArrayList<Agent> agentBiddingList; // records agents who intend to bid on this task
	private boolean hasAssigned;
	private int taskFinishedFlag; // -1 falied, 0 unfinished, 1 finished
	private int numFinishedSubtasks; //keep count of number of finished subtask, if the number equals to total number of subtasks, then this task is finished
	private Blackboard bb;
	private HashMap<Integer, Integer> subtaskQualificationAllAgentsMap;//it maps the subtaskId to the number of qualified agents for this subtask, it counts all the qualified agents in the environment
	private HashMap<Integer, Integer> subtaskQualificationBiddingAgentsNumMap;//it maps the subtaskId to the number of qualified agents for this subtask, concerning all the agents qualified agents who have bid on this task
	
	
	/**
	 * this is for RejectA,RejectB
	 * if agent bid for the task, then it is at least qualify for one of the subtasks, if the subtask needs 5 person, and this agent ranked in the top 5,
	 * then its id will be recored here.
	 * when this task failed to auction off, agent needs to check for this Map to see what is the rejection reason.
	 * if the agent is in this map, then the reason is RejectB (not enough qualified agents bid for this task, not because of this agent itself), if the 
	 * agent is not in this map, then the rejection reason is RejectA(because itself is not competitive, there are better agents for the subtask, because this agent bid for this task, 
	 * it must qualify for at least one of the subtasks, if it did not get selected, then it is because of the competitors.)
	 * NOTE: even when the task is auctioned off, if the agent is not in the map, then the reason is still RejectA. The task could get auctioned off, but this agent did not get any assignment due to 
	 * there are better qualified agent.
	 */
	private HashMap<Integer, ArrayList<Integer>> subtaskWiningAgentMap= new HashMap<Integer, ArrayList<Integer>>();//SubtaskId-->[agentId,...]   
	private HashMap<Integer,ArrayList<Agent>> subtaskQualificationBiddingAgentsMap= new HashMap<Integer, ArrayList<Agent>>();//SubtaskId-->[agent,...]   it maps the subtaskId to  an List of qualified agents for this subtask, concerning all the agents qualified agents who have bid on this task

	private double sum2=0;	//this is the sum of each qualThresh of a subtask times its number of required agent
	private double sumQuality=0; // this is the sum of the quality requirements of each subtask
	
	/**
	 * the follwing are used for calculate P_wb and P_off and find the nearest k neighbor of the task
	 */
	int RejectA=0;
	int RejectB=0;
	int bidSubmittedAndWon=0;
	double distance=0;//use Euclidean distance to measure the similarity of tasks(BlackboardMessage).
	boolean auctionedOff=false;
	
	/**
	 * use this constructor to make a copy of the messge that the agent bids and store it for finding nearest k neighbor  
	 * @param task
	 * @param subtasks
	 */
	public BlackboardMessage(Task task, ArrayList<SubTask> subtasks){
		//make shallow copy of the tasks, since it will never change
		this.task=task;
		this.subtasks=subtasks;
		this.RejectA=0;
		this.RejectB=0;
		this.bidSubmittedAndWon=0;
		this.distance=0;//use Euclidean distance to measure the similarity of tasks(BlackboardMessage).	
	}
	
	
	
	/**
	 * Create a new instance of BlackboardMessage
	 */
	public BlackboardMessage(Task task, ArrayList<SubTask> subtasks,Blackboard b){
		this.task = task;
		this.subtasks = subtasks;
		this.hasAssigned = false;
		agentBiddingList = new ArrayList<Agent>();
		taskFinishedFlag = 0;
		numFinishedSubtasks = 0;
		bb=b;
		subtaskAssignment = new HashMap<Integer, ArrayList<Integer>>();//<subtaskId,array of Agentsid>
		subtaskQualificationAllAgentsMap=new HashMap<Integer, Integer>();//<subtaskId,number of agents qualified for this subtask>
		subtaskQualificationBiddingAgentsNumMap=new HashMap<Integer, Integer>();
		for(SubTask subtask : subtasks){
			subtaskAssignment.put(subtask.getId(), new ArrayList<Integer>());
		}
		
		//update sum2 and sumQuality 
		for (SubTask subtask: subtasks){
			sum2=sum2+subtask.getQuality()*subtask.getNumAgents();
			sumQuality+=subtask.getQuality();
		}
		
//		for(SubTask subtask : subtasks){
//			subtaskAssignment.put(subtask.getId(), new ArrayList<Integer>());
//		}
	}
	
	/**
	 * 
	 */
	public void removeAssignment(){
		for(SubTask subtask : subtasks){
			subtaskAssignment.put(subtask.getId(), new ArrayList<Integer>());
		}
		agentBiddingList.clear();
	}
	
	
	/**
	 * 
	 * @return
	 */
	public Task getTask() {
	    return task;
	}

	/**
	 * 
	 * @param task
	 */
	public void setTask(Task task) {
	    this.task = task;
	}

	/**
	 * 
	 * @return
	 */
	public ArrayList<SubTask> getSubtasks() {
	    return subtasks;
	}

	/**
	 * 
	 * @param subtask
	 * @return
	 */
	public ArrayList<SubTask> removeSubtask(SubTask subtask) {
		subtasks.remove(subtask);
	    return subtasks;
	} 
	
	/**
	 * 
	 * @param subtasks
	 */
	public void setSubtasks(ArrayList<SubTask> subtasks) {
		this.subtasks = subtasks;
	}	
	
	/**
	 * 
	 * @return agents assignment for each subtask
	 */
	public HashMap<Integer, ArrayList<Integer>> getAssignment(){
		return this.subtaskAssignment;
	}
	
	/**
	 * Assign agents to do subtasks
	 * @param assignment agents assignment for each subtask 
	 */
	public void setAssignment(HashMap<Integer, ArrayList<Integer>> assignment){
		this.subtaskAssignment = assignment;
		hasAssigned = true;
	}
	
	/**
	 * Add agent to agentBiddingList of this message
	 * @param agent The agent who want to involved in this task
	 */
	public void addAgentToBiddingList(Agent agent){
//		print(String.format("Add agent %d to task%d's bidding list", agent.getId(), task.getId()));
		this.agentBiddingList.add(agent);
	}
	
	/**
	 * 
	 */
	public void setSubtaskFinish(SubTask subtask){
		boolean subtaskFinished = subtask.SetNumOfAgentFinished();
		if(subtaskFinished){//if the whole subtask get finished 
			numFinishedSubtasks++;
			if(numFinishedSubtasks == subtasks.size()){
				taskFinishedFlag = 1;
				bb.setNumFinishedTasks();
				print("Task " + task.getId() + "finished");
			}
		}
	
	}
	/**
	 * 
	 */
	public void setSubtaskFailed(SubTask subtask, int agentId){
		subtask.setFailedAgentId(agentId);
		taskFinishedFlag = -1;
	}
	
	/**
	 * 
	 * @return
	 */
	public int getTaskFinishedFlag(){
		return taskFinishedFlag;
	}
	
	/**
	 * 
	 */
	public ArrayList<Agent> getAgentBiddingList(){
		return agentBiddingList;
	}
	
	/**
	 * 
	 */
	public boolean isAssigned(){
		return hasAssigned;
	}
	/**
	 * 
	 */
	public void setAssigned(){
		this.hasAssigned = true;
	}
	
	/**
	 * 
	 * @param agentId
	 * @return
	 */
	public ArrayList<SubTask> getSubtasksAssignmentByAgent(Integer agentId){
		ArrayList<SubTask> subtasksToBeExecuted = new ArrayList<SubTask>();
		for(SubTask subtask : subtasks){
			if(subtaskAssignment.get(subtask.getId()).contains(agentId)){
				subtasksToBeExecuted.add(subtask);
			}
		}
		return subtasksToBeExecuted;
	}
	
	/**
	 * 
	 * @param subtaskId
	 * @return
	 */
	public ArrayList<Integer> getSubtasksAssignmentBySubtaskId(Integer subtaskId){
		return subtaskAssignment.get(subtaskId);
	}
	
	/**
	 * 
	 */
	public void printAssignment(){
		print(String.format("Task %d assignment (Task type = %d", task.getId(),task.getType()));
		print(this.subtaskAssignment);
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

	public HashMap<Integer, Integer> getSubtaskQualificationMap() {
		return subtaskQualificationAllAgentsMap;
	}

	public void setSubtaskQualificationMap(
			HashMap<Integer, Integer> subtaskQualificationMap) {
		this.subtaskQualificationAllAgentsMap = subtaskQualificationMap;
	}

	
   public void PrintSubtaskQualificationAllAgentsMap(){
	   for (SubTask s : this.subtasks){
		   subtaskQualificationAllAgentsMap.put(s.getId(), s.getQualifiedAgentsCount());
	   }
	  
	   print("Task "+this.task.getId()+ "  subtaskQualificationAllAgentsMap ----"+subtaskQualificationAllAgentsMap);
	   //after Printing  reset the Count and clear the Map
	   resetSubtaskQualificationCount();
	   subtaskQualificationAllAgentsMap.clear();
	   
   }
   
   
   
   public void resetSubtaskQualificationCount(){
	   for (SubTask s : this.subtasks){
		   s.setQualifiedAgentsCount(0);
	   }
   }
   
   
   
   public void resetSubtaskQualificationBiddingAgentsCount(){
	   for (SubTask s : this.subtasks){
		   s.setQualifiedBiddingAgentsCount(0);
	   }
   }
   
   
   
	/**
	 * this method does two things, one for 
	 * 1. SubtaskQualificcationBiddingAgentsMap
	 * check the agents who bid for this task, to see how many of them are qualified for each subtask.
     * note that each agent who bid for the task could be qualified for multiple subtasks within the task
	 * 2. SubtaskWiningAgentMap----(this is used for Rejection Reason to figure out RejectionA or RejectionB)
	 */
   public void calculateSubtaskQualificationBiddingAgentsMapAndSubtaskWiningAgentMap(ArrayList<Agent> agentBiddingList){
	   //before Printing  reset the Count and clear the Map
	   resetSubtaskQualificationBiddingAgentsCount();
	   this.subtaskQualificationBiddingAgentsNumMap.clear();
	   this.subtaskQualificationBiddingAgentsMap.clear();
	   this.subtaskWiningAgentMap.clear();
	   
	   for (SubTask s : this.subtasks){
//		   print("Subtabsk ~~~~~~"+s.getId());
		   int numAgentSelected=0; int requiredAgents= s.getNumAgents();
		  ArrayList<Integer> subtaskWiningAgentList=new  ArrayList<Integer>();
		  ArrayList<Agent> subtaskQualifiedBiddingAgentsList=new  ArrayList<Agent>();
		   
		  Collections.sort(agentBiddingList, new AgentQualityComparator(s.getId()));
		  
		  
		   for(Agent agent: agentBiddingList){
//			   print("agents Cap >= subtask Cap ???");
//			   print("AgentID "+agent.getId()+" Cap ="+agent.getCapabilityQuality(s.getId()));
//			   print("Subtask Cap = "+s.getCap());
			   if (agent.getCapabilityQuality(s.getId())>=s.getQuality()){//agent is qualified for this subtask
//				   print("Yes>>>>>>");
				   s.addQualifiedBiddingAgentsCount();
				   
				   subtaskQualifiedBiddingAgentsList.add(agent);
				  
				   
				 //put the agent into subtaskWiningAgentList if we have not got enough qualified agents yet
				   if (numAgentSelected<requiredAgents){
					   subtaskWiningAgentList.add(agent.getId());
					 
					   numAgentSelected++;
				   }
				   
			   }
		   }
		   this.subtaskWiningAgentMap.put(s.getId(),  subtaskWiningAgentList);
		   this.subtaskQualificationBiddingAgentsNumMap.put(s.getId(), s.getQualifiedBiddingAgentsCount());
		   this.subtaskQualificationBiddingAgentsMap.put(s.getId(),   subtaskQualifiedBiddingAgentsList);
	   }
   }
   
   
   
  public void PrintSubtaskQualificationBiddingAgentsIdMap(){
	  print("Task "+this.task.getId()+ "  SubtaskQualificationBiddingAgentsIdMap ----"+this.subtaskQualificationBiddingAgentsMap);
   }
   
  
  /**
   * this method prints the qualified agents for this subtask as well as their capabilities, so we know how to choose the top agents for this subtask
   */
  public void PrintSubtaskQualificationAgentsCap(){
	
	  for (int subtaskId : this.subtaskQualificationBiddingAgentsMap.keySet()){
	
		
		  
		  ArrayList<Agent> agentList = this.subtaskQualificationBiddingAgentsMap.get(subtaskId);
		  
		  if (!agentList.isEmpty()){
			  String agentInfo="";
			  for (Agent a: agentList){
					
					 agentInfo=agentInfo+" agent "+a.getId()+" "+ a.getCapabilityQuality(subtaskId)+" | ";
				  }
			  print ("subtask "+subtaskId+ agentInfo);
		  }
		 
		 
	  }
  }
   
   /**
    * check the agents who bid for this task, to see how many of them are qualified for each subtask.
    * note that each agent who bid for the task could be qualified for multiple subtasks within the task
    * @param agentBiddingList
    */
   public void PrintSubtaskQualificationBiddingAgentsNumMap(ArrayList<Agent> agentBiddingList){
//	   for (SubTask s : this.subtasks){
////		   print("Subtabsk ~~~~~~"+s.getId());
//		   for(Agent agent: agentBiddingList){
////			   print("agents Cap >= subtask Cap ???");
////			   print("AgentID "+agent.getId()+" Cap ="+agent.getCapabilityQuality(s.getId()));
////			   print("Subtask Cap = "+s.getCap());
//			   if (agent.getCapabilityQuality(s.getId())>=s.getQuality()){
////				   print("Yes>>>>>>");
//				   s.addQualifiedBiddingAgentsCount();
//				   
//			   }
//		   }
//		   this.subtaskQualificationBiddingAgentsMap.put(s.getId(), s.getQualifiedBiddingAgentsCount());
//	   }
	   print("Task "+this.task.getId()+ "  subtaskQualificationBiddingAgentsNumMap ----"+this.subtaskQualificationBiddingAgentsNumMap);
	   //after Printing  reset the Count and clear the Map
//	   resetSubtaskQualificationBiddingAgentsCount();
//	   this.subtaskQualificationBiddingAgentsMap.clear();
	   
   }
   
   
   
   public int getNumAgentsAssigned(){
	   
	   HashSet<Integer> subtaskSelectedAgentSet = new  HashSet<Integer>();
	   for (SubTask subtask: this.subtasks){
		   subtaskSelectedAgentSet.addAll(this.subtaskAssignment.get(subtask.getId()));
//		   System.err.println("subtask"+subtask.getId()+" has "+subtaskSelectedAgentSet.size()+" agents");
//		   System.err.println("subtask"+subtask.getId()+"= "+this.subtaskAssignment.get(subtask.getId()));
	   }
	  
	   
	   return  subtaskSelectedAgentSet.size();
   }
   
   
   public void PrintSubtaskWiningAgentMap(){
	   print("Task "+this.task.getId()+ "  subtaskWiningAgentMap ----"+this.subtaskWiningAgentMap);
   }
   
   
   
   public void PrintTaskDetail(){
	   String Line="";
	   for(SubTask s: this.subtasks){
		   Line+="[Id "+ s.getId()+"|"+s.getNumAgents()+"|"+s.getQuality()+"] ";
	   }
	   print("Subtasks :"+Line);

   }
   

public HashMap<Integer, Integer> getSubtaskQualificationBiddingAgentsNumMap() {
	return subtaskQualificationBiddingAgentsNumMap;
}

public void setSubtaskQualificationBiddingAgentsNumMap(
		HashMap<Integer, Integer> subtaskQualificationBiddingAgentsNumMap) {
	this.subtaskQualificationBiddingAgentsNumMap = subtaskQualificationBiddingAgentsNumMap;
}

/** debug method */
@SuppressWarnings("unused")
private void print(String s){
	if (PrintClass.DebugMode && PrintClass.printClass){
		System.out.println(this.getClass().getSimpleName()+"::"+s);
	}else if(PrintClass.DebugMode){
		System.out.println(s);
	}
}

private void print(HashMap<Integer, ArrayList<Integer>> map){
	if (PrintClass.DebugMode && PrintClass.printClass){
		System.out.println(this.getClass().getSimpleName()+"::"+map);
	}else if(PrintClass.DebugMode){
		System.out.println(map);
	}
}

public HashMap<Integer, ArrayList<Integer>> getSubtaskSelectedAgentMap() {
	return subtaskWiningAgentMap;
}

public void setSubtaskSelectedAgentMap(HashMap<Integer, ArrayList<Integer>> subtaskSelectedAgentMap) {
	this.subtaskWiningAgentMap = subtaskSelectedAgentMap;
}

public HashMap<Integer, ArrayList<Agent>> getSubtaskQualificationBiddingAgentsMap() {
	return subtaskQualificationBiddingAgentsMap;
}

public void setSubtaskQualificationBiddingAgentsMap(
		HashMap<Integer, ArrayList<Agent>> subtaskQualificationBiddingAgentsMap) {
	this.subtaskQualificationBiddingAgentsMap = subtaskQualificationBiddingAgentsMap;
}

public double getSum2() {
	return sum2;
}

public void setSum2(double sum2) {
	this.sum2 = sum2;
}

public int getRejectA() {
	return RejectA;
}

public void setRejectA(int rejectA) {
	RejectA = rejectA;
}

public int getRejectB() {
	return RejectB;
}

public void setRejectB(int rejectB) {
	RejectB = rejectB;
}

public int getBidSubmittedAndWon() {
	return bidSubmittedAndWon;
}

public void setBidSubmittedAndWon(int bidSubmittedAndWon) {
	this.bidSubmittedAndWon = bidSubmittedAndWon;
}

public double getDistance() {
	return distance;
}

public void setDistance(double distance) {
	this.distance = distance;
}



public boolean isAuctionedOff() {
	return auctionedOff;
}



public void setAuctionedOff(boolean auctionedOff) {
	this.auctionedOff = auctionedOff;
}



public double getSumQuality() {
	return sumQuality;
}



public void setSumQuality(double sumQuality) {
	this.sumQuality = sumQuality;
}






}
