/**
 * 
 */
package AdhocCollaboration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;

import cern.jet.random.Uniform;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.parameter.Parameters;
import repast.simphony.util.collections.IndexedIterable;


/** MainAgent is responsible for:
 * 	- introducing the tasks 
 *  - dividing each task into subtasks(with cap, minNumAgents, quality)
 *  - populating the blackboard with tasks/subtasks
 *  step() method is a scheduled method(method is invoked on instances of this class)
 *  step() is called every iteration of the simulation on all Agents starting at tick 1 and every tick after that
@author Bin Chen 
@author Xi Chen
*/
public class MainAgent {

	private int lastTaskId; 
	protected static int tick;
	private int totalTick; // use this and agentOpenness to calculate at which tick to kill one agent;
	private double taskOpenness;
	private Context context;
//	private ArrayList<LearningMatch> learningTeaching;
//	private ArrayList<Double> averageQualityList;
//	private HashMap<Integer,SubTask> agentMatch;//$$$$$$matchs agent with subtask
	private Blackboard blackboard;
	private Parameters params;
	private int agentCount;//total number of agents in this simulation
	private int agentTypeScenario;
//	private int prevFinishedSubtasks;
//	private int prevFinishedTasks;
	private double agentOpenness;
	private double agentQualityMax;
	public static Uniform uniform;
	public static int randomSeed;
	private ArrayList<Agent> killingQueue;//put selected agents to the killingQueue and start kill them in the next tick. Agent is killed if they are not busy, if busy then try to kill in the tick 
	private LinkedList<BlackboardMessage> returnedMessages;
	private int lastAgentId;
//	private int tickToKill; // the time interval to kill an agent
//	private int tickToKill2;//use this to do the second round kill to maintain the preset agent openness
	private int initalCapNum;//Number of  capabilities agents have when created
	private static int NonKillAgentId1;//Set the agentId here so that it won't be killed during the simulation to see how they perceive AO and TO
	private static int NonKillAgentId2;
	private static int NonKillAgentId3;
	private int ticksToFinish;//ticks to finish a task
	

	
	private int haveAddedToKillingQueue=0;//the number of agents has been added to killingQueue so far;
	private double killPerTick;//how many agents need to be killed in one tick.
	private double  shouldHaveKilledAtThisTick;
	
	/*user enter a string of two numbers, not separated by comma. Example: 3333  .this means 33% of HardTask, 33% of AverageTask, and 100-(33+33) % of EasyTask*/
	private String TaskDistrubution;// the string that user entered which specifies the task distribution, we use this to pick the congfic file to read the task order
	private String hardTaskPerString;
	private String averageTaskPerString;
	private static int InitalTaskNumber=30;// the number of tasks that we wanted to introduce in the environment before the simulation starts, when the simulation starts, we introduce 1 new task at each tick
	
	Properties configTask;
	Properties configFile; 
	
	/**
	 * Disable agentTypes function by entering an 0, this way agent will be randomly generated, we don't care about agent types. After kill, we also randomly entroduce an agent 
	 */
	private int UsingAgentDistrubution;
	
	/**
	 * Disable task Types function by entering an 0, this way all tasks are randomly generated, they are no task types, we use"config_typesRandom.properties" to draw all the tasks from
	 */
	private int UsingTaskDistrubution;
	
	/**
	 * we have changed AO implementation. 
	 * 1. Before AO means after the simulation how many percent of the total agent need to be replaced.
	 * 2. Now, we say AO is the percentage of the total agents that needs to be replaced in one tick.
	 * 3. each tick agent roll a dice, if the result ,[0,1], is less than or equal to AO, then the agent leave,
	 * so here AO means the chance of each agent leaving at each tick.  (2/14/2016)
	 * AOimplementation=1 old implementation, AOimplementation=2 (new implementation, percentage to replace at each tick)
	 * This only affects "killPerTick" in the constructor, 
	 * AOimplementation=3,  the chance of each agent leaving at each tick
	 */
	private int AOimplementation=3;
	
	
	/**
	 * We have changed TO implementation. 
	 * TOimplementation=1 old implementation: intially post 30 or 40 tasks, then each tick post 1 tasks, the tasks
	 * get auctioned off are gone, the ones did not get auctioned off are reposted to the blackboard for auction together with the newly posted 1 task at each tick
	 * TOimplementation=2 new implementation (Used in AAMAS2016)
	 * we maintain a task pool of 100 tasks, and every tick we randomly choose 20 from it to post on blackboard for agents
	 * to bid on, (the chosen 20 tasks will not be reposted no matter they are auctionedd off or not) there are no repost at all.
	 * the TO tells how many percent of the tasks in the task pool (100 tasks) needs to be replaced per tick 
	 * TOimplementation=3 
	 * we maintain a task pool of 100 tasks, and every tick we post the 100 tasks for biding, the TO changes the percentage of tasks in this task pool, No reposting.
	 * TOimplementation=4  (2/14/2016)
	 * we maintain a task pool of 20 tasks, and every tick we post the 20 tasks for biding, at every tick, each task roll a dice, if the result <= TO, then this task will be replaced with new tasks 
	 */
	private int TOimplementation=4;
	/*
	 * the following are used in new TOimplementation
	 */
	/**
	 * // readTaskimplementation=1 opens config_typesRandom.properties file and use it to fetch tasks when needed. 
	 * readTaskimplementation=2 read in the config_typesRandom.properties file and create all the tasks and store it in this list
	 */
	int readTaskimplementation=1;
	
	int totalNumOfTasks = 15504;
	int sampledTasksSize=20;
	int taskPoolSize=20;
	double shouldHaveReplacedAtThisTick = 0;
	double haveReplaced = 0;
	int numTasksPosted=0;
	/*
	 * current task pool contains 100 integers representing tasks 
	 */
	ArrayList<Integer> tasksInPool = new ArrayList<Integer>();
	
	/*
	 * current task pool contains 20 integers representing tasks 
	 */
	ArrayList<Integer> tasksSampledPool = new ArrayList<Integer>();
	
	/**
	 * use this task map to store the tasks, so we don't need to create old tasks, just feth it from here
	 */
	HashMap<Integer,Task> TaskMap= new HashMap<Integer,Task>();
	
	/*
	 * use it to contain the unique Tasks posted
	 */
	 HashSet<Integer> uniqueTaskPosted= new HashSet<Integer>();
	
	 /**
	  * use it to contain the unique Tasks in tasksInpool list
	  */
	 HashSet<Integer> uniqueTaskInPool= new HashSet<Integer>();
	 
	 int numAgentsIntroduced=0;
	
	/**
	 * read in the config_typesRandom.properties file and create all the tasks and store it in this list
	 */
	 public static ArrayList<Task> TaskList = new ArrayList<Task>();
	
	
	 public static FileWriter writer=null;
	 public static FileWriter writer1=null;
	 public static FileWriter agentCapWriter=null;
	 public static FileWriter taskDetailWriter=null;
	 public static FileWriter agentUsolveUlearnWriter=null;
	 
	
	 /**
	  * contains the learning coefficient 
	  */
	 public static SubtaskLearningType subtaskLearningType=new SubtaskLearningType();
	 
    
    /** Creates a new instance of Model 
     * @throws IOException */
    public MainAgent(Blackboard bb, double task_openness,int total_tick, int agent_count,int agent_types_scenario,double agent_openness,int initalCapNummber,String HardPercentageStr, String AveragePercentageStr) throws IOException {
//    	random = new Random();
    	print(" Main agent is called");
    	initalCapNum=initalCapNummber;
    	lastTaskId 		= 0;
    	tick 			= 0;
    	taskOpenness	= task_openness;
    	blackboard 		= bb;   
    	params 			= RunEnvironment.getInstance().getParameters();
    	totalTick       =(Integer) params.getValue ("total_tick");
    	agentCount 		= (Integer) params.getValue ("agent_count");
    	agentTypeScenario = (Integer) params.getValue("agent_types_scenario");
    	NonKillAgentId1 =(Integer) params.getValue("NonKillAgentId1");
    	NonKillAgentId2 =(Integer) params.getValue("NonKillAgentId2");
    	NonKillAgentId3 =(Integer) params.getValue("NonKillAgentId3");
    	numAgentsIntroduced=agentCount;
//    	prevFinishedSubtasks=0;
//    	prevFinishedTasks=0;
//    	agentOpenness 	= (Double) params.getValue("agent_openness");    
    	agentOpenness=agent_openness;
    	agentQualityMax = 1;
    	killingQueue = new ArrayList<Agent>();
    	returnedMessages = new LinkedList<BlackboardMessage>();
    	lastAgentId = agent_count;
    	UsingTaskDistrubution=(Integer) params.getValue ("UsingTaskDistrubution");
    	
//    	tickToKill = (int) Math.ceil(totalTick / (agentOpenness * agentCount));
//    	tickToKill2=getTickToKill2();
    	if (AOimplementation==1){
    		killPerTick= (agentOpenness * agentCount)/totalTick;
    	}else if(AOimplementation==2){
    		killPerTick=agentOpenness*agentCount;
    	}
    	
    	
    	
    	if (this.UsingTaskDistrubution==1){
    		print("Using Task distrucbution!!!!!!!!!!!!! ");
    		
        	this.hardTaskPerString=HardPercentageStr;
        	this.averageTaskPerString=AveragePercentageStr;
        	
        	configTask = new Properties();
        	try {
//    			configTask.load(this.getClass().getClassLoader().getResourceAsStream("config_task"+taskOpenness+".properties"));
        		configTask.load(this.getClass().getClassLoader().getResourceAsStream("config_task"+"_TO"+taskOpenness+this.hardTaskPerString+this.averageTaskPerString));
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
        	
        	// get the task specifics from the configuration file
        	 configFile = new Properties();
        	try {
    			configFile.load(this.getClass().getClassLoader().getResourceAsStream("config_types.properties"));
    		} catch (IOException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
    	}else if (this.UsingTaskDistrubution==0){
    		print("NOT Using Task distrucbution!!!!!!!!!!!!! ");
    		/*
    		 * No task types are used here, all tasks are randomly generated  we use"config_typesRandom.properties" to draw all the tasks from.
    		 */
    		if (readTaskimplementation==2){
    			readInFileAndCreateTasks();
    		}else{
    			// get the task specifics from the configuration file
	           	 configFile = new Properties();
	           	try {
	       			configFile.load(this.getClass().getClassLoader().getResourceAsStream("config_typesRandom.properties"));
	       		} catch (IOException e) {
	       			// TODO Auto-generated catch block
	       			e.printStackTrace();
	       		}
    			
    		}
    		
    		
    		
    	}
    	
    	
    	
    	/** open an writer */
    	if (OutputClass.agentOutput){
    		File AgentOutput;
    		if (this.blackboard.getOption()==99){
    			AgentOutput = new File(OutputClass.direcotry+"/AgentOutput_AO"+this.agentOpenness+"TO"+this.taskOpenness+"Op"+this.blackboard.getOption()+"_"+this.blackboard.getOptionTypeDistrubution()+"_"+Blackboard.getCurrentTimeStamp()+".txt");
    		}else{
    			AgentOutput = new File(OutputClass.direcotry+"/AgentOutput_AO"+this.agentOpenness+"TO"+this.taskOpenness+"Op"+this.blackboard.getOption()+"_"+Blackboard.getCurrentTimeStamp()+".txt");
    		}
    			
    		
    		MainAgent.writer = new FileWriter(AgentOutput);
    		writer.write("tick,Id,NumTaskInvolved,TaskAssignmentAtOneTick,Reward,TaskTobidAtOneTick,NumBidsSubmitted,NumbidsSubmittedAndWon,SelectedForCurrentBid,"
    				+ "run,TaskTypebidAtOneTick,random_seed,AgentOpenness,TaskOpenness,Option,NumAgentsAssigned,NumAgentsRequired,"
    				+ "RewardAtCurrentTick,NumSubtasksAssignedAtOneTick,TaskReward,SelfGainAtOneTick,ObservationGainAtOneTick,"
    				+ "ObservedCapUsedCount,ObervatedCapUsedAtOneTick,OptionType\n");
    	}
    	
    	
    	if (OutputClass.blackboardOutput){
    		File BlackboardOutput = new File(OutputClass.direcotry+"/BBoutput_AO"+this.agentOpenness+"TO"+this.taskOpenness+"Op"+this.blackboard.getOption()+"_"+Blackboard.getCurrentTimeStamp()+".txt");
    		MainAgent.writer1=new FileWriter(BlackboardOutput);
    		writer1.write(this.blackboard.getBlackboardOutputTitle()+"\n");
    	}
    	
    	
    	if (OutputClass.agentCap){
    		File AgentCap = new File(OutputClass.direcotry+"/AgentCap_AO"+this.agentOpenness+"TO"+this.taskOpenness+"Op"+this.blackboard.getOption()+"_"+Blackboard.getCurrentTimeStamp()+".txt");
    		MainAgent.agentCapWriter = new FileWriter(AgentCap);
    		agentCapWriter.write("Id,tickIn,qualityList\n");
    	}
    	
    	
    	
    	if (OutputClass.taskDetail){
    		File taskDetail = new File(OutputClass.direcotry+"/taskDetail_AO"+this.agentOpenness+"TO"+this.taskOpenness+"Op"+this.blackboard.getOption()+"_"+Blackboard.getCurrentTimeStamp()+".txt");
    		MainAgent.taskDetailWriter = new FileWriter(taskDetail);
    		taskDetailWriter.write("taskId,taskType,tickPosted,subtask1,subtask2,subtask3,subtask4,subtask5\n");
    	}
    		
    	if (OutputClass.agentUsolveUlearn){
    		if (this.blackboard.getOption()==14){
    			File agentUsolveUlearn = new File(OutputClass.direcotry+"/agentUsolveUlearn_AO"+this.agentOpenness+"TO"+this.taskOpenness+"Op"+this.blackboard.getOption()+"_"+Blackboard.getCurrentTimeStamp()+".txt");
        		MainAgent.agentUsolveUlearnWriter = new FileWriter(agentUsolveUlearn);
        		agentUsolveUlearnWriter.write("agentId,taskId,taskType,tick,EU_Solve,EU_learn\n");
    		}
    		
    	}	
    	
    	
    	
    		
    	
    	print("********* Task openness "+taskOpenness+"     Agent openness "+agentOpenness+"  Total agents needs to be killed = "+(agentOpenness * agentCount));
    }
    
    /** 
     * Controls what is done during each tick of the simulation. 
     * NOTICE: We get rid of the triggering stuff. now everything is managed in mainAgent step();
     * @throws IOException 
     **/
    @ScheduledMethod(start=1, interval=1) // duration = num_ticks
    public void step() throws IOException {    	
    	tick++;
    	print("~~~~~~~~~ Current tick before loop is "+tick);
      	if(tick <= this.totalTick){
    		
        	print("~~~~~~~~~ Current tick is "+tick);
        	//do normal steps
        	

        	
        	
        	/**
        	 * post tasks and replace task pools according to TO
        	 */
        	if (this.TOimplementation==1){
        		Task newTask = findNewTask();
            	postMessages(newTask);//this triggers agent to act
        	}else if (this.TOimplementation==2){
        		//postNtasks
        		postNtasks();
        
        	}else if (this.TOimplementation==3){
        		postNtasksNoSampling();
        	}else if (this.TOimplementation==4){
        		 postNtasksNewTO();
        	}
//        
//        	
//        	
//        	/**
//        	 * call agents to bid
//        	 */
        	for (Agent a :  this.blackboard.getAgentList()){
        		a.agentSelectTask();
        	}
//        	
//        	
//        	
//        	/**
//        	 * call auction
//        	 */
        	holdAuction();
//        	
//        	
//        	/**
//        	 * call agent read assignment, execute task, update cap and reward, agent leave according to AO
//        	 */
        	for (Agent a :  this.blackboard.getAgentList()){
        		a.readAssignment();
        		a.agentExecuteSubtasks_updateCap_leave();
        	}
//        	
//        	
//        	/**
//        	 * remove agent and add new agent 
//        	 */
        	print("AgentList size = "+this.blackboard.getAgentList().size());
        	print("killing start. agentKillList size = "+this.blackboard.agentKillList.size());
        	
        	for (Agent a : this.blackboard.agentKillList){
        		
        		
        		if (!a.getBusy()){
        			//remove agent
        			int killedAgentType = a.getAgentType();
					int opType = a.optionType;
					this.blackboard.getKilledAgentSet().add(a.getId());
					this.blackboard.getAgentList().remove(a);
					print("removed agent "+ a.getId());
					getContext().remove(a);
					
					
					//adding new agent
	        		numAgentsIntroduced++;
					addAgent(killedAgentType, opType);
        		}
        	}
        	//clear the list
        	
    		this.blackboard.agentKillList.clear();
    		print("******killing done! agentKillList size = "+this.blackboard.agentKillList.size());
        	print("AgentList size = "+this.blackboard.getAgentList().size());
    		
    		
    		
        	
        	/**
        	 * output bb info
        	 */
    		if (OutputClass.blackboardOutput){
    			try {
        			MainAgent.writer1.write(this.blackboard.getBlackboardOutput()+"\n");
        		} catch (IOException e) {
        			// TODO Auto-generated catch block
        			e.printStackTrace();
        		}
    		}
        	
        	
//        	print("Finished Task = "+this.blackboard.getNumFinishedTasks());
        	print(" Tasks AuctionedOff Total = "+this.blackboard.getNumTasksAuctionedOff());
        	
    	}
      	else{
        	print("~~~~~~~~~ Current tick is "+tick);
        	print("!!!!!!!!!!!!!!!!!!!!!!!!!!!END");
    		if (this.blackboard.getOption()==99){
    			System.out.println("AO="+this.agentOpenness+" TO="+this.taskOpenness+" OptionDistrubution="+this.blackboard.getOptionTypeDistrubution());
    		}else{
    			System.out.println("AO="+this.agentOpenness+" TO="+this.taskOpenness+" Option="+this.blackboard.getOption());
    		}
    		
    		double shouldKill=agentOpenness * agentCount;
    		if(this.AOimplementation==2){
    			shouldKill=agentOpenness * this.totalTick;
    		}
    		print("Total # agents have been putting in KillingQueue = "+haveAddedToKillingQueue+"   Should kill "+shouldKill);
    		
    		
    		
    		
   
    		
    		
    		
    		if (OutputClass.blackboardOutput){
    			MainAgent.writer1.write(this.blackboard.getBlackboardOutput()+"\n");
        		MainAgent.writer1.close();
        		
        		
         		/**
        		 * close writer for agent output
        		 */
        		try {
    				MainAgent.writer.close();
//    				
    			} catch (IOException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
        		
        		if (OutputClass.agentCap){
        			MainAgent.agentCapWriter.close();
        		}
        	
        		if (OutputClass.taskDetail){
        			MainAgent.taskDetailWriter.close();
        		}
        		
        		
        		if (OutputClass.agentUsolveUlearn){
        			if (this.blackboard.getOption()==14){
            			MainAgent.agentUsolveUlearnWriter.close();
            		}
        		}
        		
        		
    		}
    		
			RunEnvironment.getInstance().endRun();
			
      	}
    	print("go to next round!");
    }
    
    
    
    
    private void postNtasks() {
		if (tick==1){
			//InitializeTaskPool and sample 20 tasks
			InitializeTaskPool();
		}
		
		if (readTaskimplementation==2){
			actualPosting2();		
		}else{
			//this post the 20 sampled task from tasksSampledPool
			actualPosting1();
		}
			
		
		//replace the tasks in the task pool 
		replaceTaskPool();
		sampleTasks();		
	
//	    	print(" sample random 20 tasks to returnedMessages list, wait to be posted on blackboard as returned messages" );
		
		updatingTaskPostingInfoOnBlackboard();
		}
    
    
    
    private void postNtasksNoSampling(){
    	if (tick==1){
    		
    		InitializeTaskPoolNoSampling();
		}
		
		if (readTaskimplementation==2){
			actualPosting2();		
		}else{
			
			actualPostingTasksInPool();
		}
			
		
		//replace the tasks in the task pool 
		replaceTaskPool();
//		sampleTasks();		
	
		
		updatingTaskPostingInfoOnBlackboard();
    }
    
    
    private void postNtasksNewTO(){
    	if (tick==1){
    		
    		InitializeTaskPoolNoSampling();
		}
		
		if (readTaskimplementation==2){
			actualPosting2();		
		}else{
			
			actualPostingTasksInPool();
		}
			
		
		//replace the tasks in the task pool 
		replaceTaskPoolNewTO();
	
		
		updatingTaskPostingInfoOnBlackboard();
    }
    
    
    
    
    private void replaceTaskPoolNewTO(){
		// replace taskpool according to TO
    	
    	ArrayList<Integer> tasksToRemove =new ArrayList<Integer>();
    	for (int task : tasksInPool){
    		//roll a dice to decide if replace or not
    		double x=MainAgent.uniform.nextDoubleFromTo(0, 1.0);
    		if (x<=this.taskOpenness){//replace
    			
    			tasksToRemove.add(task);
//    			tasksInPool.remove(task);
    		}
    	}
    	
    	for (int t : tasksToRemove){
    		
    		tasksInPool.remove(new Integer( t));
    		//add new task
    		// adding new ones into the pool
    		int num = MainAgent.uniform.nextIntFromTo(0,totalNumOfTasks-1);  // from [0,15503]
    		while (this.uniqueTaskInPool.contains(num)) {
    			num =  MainAgent.uniform.nextIntFromTo(0,totalNumOfTasks-1);
    		}
    		tasksInPool.add(num);
    		print("replaced task"+t+" in taskpool for a new task--"+num);
//    		uniqueTaskPosted.add(num);
    		haveReplaced++;
    	}
    	
    	
    	
    	
    	
    	
	}
    
    
    
    
    
		

private void updatingTaskPostingInfoOnBlackboard(){
	blackboard.setNumTaskPosted(this.numTasksPosted);
	blackboard.setNumUniqueTaskPosted(this.uniqueTaskPosted.size());
//	System.out.println(this.uniqueTaskPosted.size());
}
    
    
//	private void chooseNtasks() {
//		taskToPost.clear();
//		for (int j = 0; j < numToPost; j++) {
//			int index = random.nextInt(numToPost); // from [0,19]
//			int target=tasksInPool.get(index);
//			taskToPost.add(target);
//			numTasksPosted++;
//			
//			if (uniqueTaskPosted.contains(target)){
//				numRepeatedtaskPosted++;
//			}
//			
//		}
//		uniqueTaskPosted.addAll(taskToPost);
//
//}
	
	
	private void replaceTaskPool(){
		// replace taskpool according to TO
		double replacePertick=taskOpenness *taskPoolSize;
		shouldHaveReplacedAtThisTick = replacePertick* tick;
		double needReplace = shouldHaveReplacedAtThisTick - haveReplaced;
		int needReplaceRoundDown = (int) Math.floor(needReplace);
		if (needReplaceRoundDown >= 1) {
			// need to replace multiple tasks in one tick

			for (int i = 0; i < needReplaceRoundDown; i++) {
				// remove from taskpool
//				int index = random.nextInt(100); // from [0,99]
				int index = MainAgent.uniform.nextIntFromTo(0, 99);
				int replacedTask=tasksInPool.get(index);
				tasksInPool.remove(index);

				// adding new ones into the pool
				int num = MainAgent.uniform.nextIntFromTo(0,totalNumOfTasks-1);  // from [0,15503]
				while (this.uniqueTaskInPool.contains(num)) {
					num = MainAgent.uniform.nextIntFromTo(0,totalNumOfTasks-1);
				}
				tasksInPool.add(num);
				print("replaced task"+replacedTask+" in taskpool for a new task--"+num);
//				uniqueTaskPosted.add(num);
				haveReplaced++;
			}
		} else {// need to kill one after several tick
		}
	}
    
	
	public void sampleTasks(){
		
    	/**
    	 * choose 20 tasks from the taskInPool list to put into sampledTaskPool
    	 */
    	this.tasksSampledPool.clear();
    	for (int i=0;i<this.sampledTasksSize;i++){
//    		int index = random.nextInt(100);//from [0,99]
    		int index=MainAgent.uniform.nextIntFromTo(0,99);
    		int targetTask=tasksInPool.get(index);
    		while (this.tasksSampledPool.contains(targetTask)){
    			 index = MainAgent.uniform.nextIntFromTo(0,99);//from [0,99]
        		 targetTask=tasksInPool.get(index);
    		}
    		this.tasksSampledPool.add( targetTask);
    		uniqueTaskPosted.add(targetTask);
    		//TODO
    	
    	}
    	print("sampledTasks="+this.tasksSampledPool);
	}
    
	
	/**
	 * this method read fetches tasks in TaskList (contains all tasks) instead of form the config_typesRandom.properties file and  post them on blackbaord to triger the auction
	 */
	private void actualPosting2(){
		
		//posting these tasks to blackboard


	
			ArrayList<BlackboardMessage> messagesToPost = new ArrayList<BlackboardMessage>();
			//finding tasks from the file and decompose them into tasks
			
			
			for (int j :tasksInPool){
				numTasksPosted++;
//				print("taskToPost list size = "+tasksInPool.size());
				
				Task task=TaskList.get(j);
	    		/*
	    		 * package the task into blackboardMessage and add them to messagesToPost.
	    		 */
	    		messagesToPost.add(new BlackboardMessage(task, task.getSubtasks(),blackboard));
	    		}
			
			//posting
	    	blackboard.post(messagesToPost); 
			
	}
	
	
	
/**
 * this method fetches tasks in the config_tupesRandom.properties file and post 	
 */
private void actualPosting1(){
		
		//posting these tasks to blackboard


	
			ArrayList<BlackboardMessage> messagesToPost = new ArrayList<BlackboardMessage>();
			//finding tasks from the file and decompose them into tasks
			
			ArrayList<Integer> taskNeedsTobeCreateList = new ArrayList<Integer>();
			
			for (int i: tasksInPool){//check which tasks need to be created
				if (!TaskMap.containsKey(i)){
					taskNeedsTobeCreateList.add(i);
				}
			}
			
			
			
			for (int j: taskNeedsTobeCreateList){//create the new tasks
				String task_type = Integer.toString(j+1); 	//[1,10054] fetch in config file for task type

//	        	String num_subtasks = configFile.getProperty("Num_Subtasks"+task_type);
	        	String subtasks  = configFile.getProperty("Subtasks"+task_type);	
	        	String Num_Agents = configFile.getProperty("Num_Agents"+task_type);
	        	String Quality = configFile.getProperty("Quality"+task_type);  	       	
	        	//print("s="+num_subtasks+" g="+subtasks);
	        	
	    		Task task = new Task();
	    		task.setId(++lastTaskId);
	    		task.setType(Integer.parseInt(task_type));
//	    		task.setNumSubtasks(Integer.parseInt(num_subtasks));
	    		task.setNumSubtasks(5);
	    		//print("******** num subtasks "+Integer.parseInt(num_subtasks));
	    		print(" New task id "+task.getId());
//	    		print("num_subtasks"+num_subtasks+" subtasks"+subtasks+" Num_Agents"+Num_Agents+" Quality"+Quality);
	    		print("num_subtasks"+5+" subtasks"+subtasks+" Num_Agents"+Num_Agents+" Quality"+Quality);
	    	
	    		// decompose task into subtasks
	    		//**************************************************
	    		ArrayList<Integer> subtasksList = new ArrayList<Integer>();
	    		String [] subtasksString = subtasks.split(",");
	    		//change it to integer array 
	    		for(int i=0; i<subtasksString.length; i++) {
	    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
	    			subtasksList.add(Integer.parseInt(subtasksString[i]));
	    		}
	    		
	    		ArrayList<Integer> Num_AgentsList = new ArrayList<Integer>();
	    		String[] Num_AgentsString = Num_Agents.split(",");
	    		for(int i=0; i<Num_AgentsString.length; i++) {
	    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
	    			Num_AgentsList.add(Integer.parseInt(Num_AgentsString[i]));
	    		}
	    		   		
	    		ArrayList<Double> QualityList = new ArrayList<Double>();
	    		String[] QualityString = Quality.split(",");
	    		for(int i=0; i<QualityString.length; i++) {
	    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
	    			QualityList.add(Double.parseDouble(QualityString[i]));
	    		}
	 
	    		task.decompose(subtasksList,Num_AgentsList,QualityList);
	    		TaskMap.put(j, task);
	    		
	    		
	    		
			}
			
			/**
			 * post the 20 tasks in the tasksSampledPool
			 */
			for (int k: this.tasksSampledPool){
				numTasksPosted++;
				
				Task task = TaskMap.get(k);
				task.setId(++lastTaskId);
				
				
				/*
				 * output the details of the posted 20 tasks into a file
				 */
				outputTaskDetailToFile(task);
				
				
//				System.out.println(task.getId());
				
	    		/*
	    		 * package the task into blackboardMessage and add them to messagesToPost.
	    		 */
	    		messagesToPost.add(new BlackboardMessage(task, task.getSubtasks(),blackboard));
				
			}
		
			//posting
	    	blackboard.post(messagesToPost); 
			
	}
	


private void actualPostingTasksInPool(){
	
	//posting these tasks to blackboard



		ArrayList<BlackboardMessage> messagesToPost = new ArrayList<BlackboardMessage>();
		//finding tasks from the file and decompose them into tasks
		
		ArrayList<Integer> taskNeedsTobeCreateList = new ArrayList<Integer>();
		
		for (int i: tasksInPool){//check which tasks need to be created
			if (!TaskMap.containsKey(i)){
				taskNeedsTobeCreateList.add(i);
			}
		}
		
		
		
		for (int j: taskNeedsTobeCreateList){//create the new tasks
			String task_type = Integer.toString(j+1); 	//[1,10054] fetch in config file for task type

//        	String num_subtasks = configFile.getProperty("Num_Subtasks"+task_type);
        	String subtasks  = configFile.getProperty("Subtasks"+task_type);	
        	String Num_Agents = configFile.getProperty("Num_Agents"+task_type);
        	String Quality = configFile.getProperty("Quality"+task_type);  	       	
        	//print("s="+num_subtasks+" g="+subtasks);
        	
    		Task task = new Task();
//    		task.setId(++lastTaskId);
    		task.setType(Integer.parseInt(task_type));
//    		task.setNumSubtasks(Integer.parseInt(num_subtasks));
    		task.setNumSubtasks(5);
    		//print("******** num subtasks "+Integer.parseInt(num_subtasks));
//    		print(" New task id "+task.getId());
//    		print("num_subtasks"+num_subtasks+" subtasks"+subtasks+" Num_Agents"+Num_Agents+" Quality"+Quality);
//    		print("num_subtasks"+5+" subtasks"+subtasks+" Num_Agents"+Num_Agents+" Quality"+Quality);
    	
    		// decompose task into subtasks
    		//**************************************************
    		ArrayList<Integer> subtasksList = new ArrayList<Integer>();
    		String [] subtasksString = subtasks.split(",");
    		//change it to integer array 
    		for(int i=0; i<subtasksString.length; i++) {
    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
    			subtasksList.add(Integer.parseInt(subtasksString[i]));
    		}
    		
    		ArrayList<Integer> Num_AgentsList = new ArrayList<Integer>();
    		String[] Num_AgentsString = Num_Agents.split(",");
    		for(int i=0; i<Num_AgentsString.length; i++) {
    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
    			Num_AgentsList.add(Integer.parseInt(Num_AgentsString[i]));
    		}
    		   		
    		ArrayList<Double> QualityList = new ArrayList<Double>();
    		String[] QualityString = Quality.split(",");
    		for(int i=0; i<QualityString.length; i++) {
    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
    			QualityList.add(Double.parseDouble(QualityString[i]));
    		}
 
    		task.decompose(subtasksList,Num_AgentsList,QualityList);
    		TaskMap.put(j, task);
    		
    		
    		
		}
		
		/**
		 * post the 100 tasks in the tasksInPool
		 */
		for (int k: this.tasksInPool){
			numTasksPosted++;
			
			Task task = TaskMap.get(k);
			task.setId(++lastTaskId);
			
			print("post task id = "+task.getId() + "task type = "+task.getType() + "detail: "+task.getTaskDetial());
			
			/*
			 * output the details of the posted  tasks into a file
			 */
			outputTaskDetailToFile(task);
			uniqueTaskPosted.add(task.getType());
			
//			System.out.println(task.getId());
			
    		/*
    		 * package the task into blackboardMessage and add them to messagesToPost.
    		 */
    		messagesToPost.add(new BlackboardMessage(task, task.getSubtasks(),blackboard));
			
		}
	
		//posting
    	blackboard.post(messagesToPost); 
		
}

	
	private void outputTaskDetailToFile(Task t) {
		/**
		 * output into file
		 */
		if (OutputClass.taskDetail){
			try {
				MainAgent.taskDetailWriter.write(t.getTaskDetial()+"\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	
}

	/**
     * this method it used to maintain AO, it will determine if current tick needs to kill agents, if needs then add them to KillingQueue and then kill the ideal agents.
     * According to AOimplementation=1 or 2, we use different implementation
     */
    private void maintainAO(){
    	
    	//adding agent to kill Queue
    	
    		shouldHaveKilledAtThisTick= killPerTick* tick;
        	double needKill=	shouldHaveKilledAtThisTick-	haveAddedToKillingQueue;
    		int needKillRoundDown = (int) Math.floor(needKill);
    		if (needKillRoundDown>=1){
    			//need to kill multiple agents in one tick
    			
    			for (int i=0;i<needKillRoundDown;i++){
    				//add to killingQueue
    				Agent selectedAgent = getOneAgent();
    	        	while(killingQueue.contains(selectedAgent)){
    	        		selectedAgent = getOneAgent();
    	        	}
    	        	killingQueue.add(selectedAgent);
    	        	this.blackboard.killingQueue.add(selectedAgent.getId());
    	        	print(String.format("Add agent %d into killing queue", selectedAgent.getId()));
    				
    				haveAddedToKillingQueue++;
    			}
    		}
    		else{//need to kill one after several tick	
    		}
		
		
		
		
		if (this.blackboard.killAgentImplementation==1){
			//do actual kill 
	    	for(Agent a : new ArrayList<Agent>(killingQueue)){
	    		//Only kill idle agents
	    		if(!a.getBusy()){
	    			print("%%%%%%%%%%%%%%%%%Killing%%%%%%%%%%%%%%%%%");
	    			int killedAgentType=a.getAgentType();/* 1 - expert, 2 - average ,3-novice*/
	    			int opType=a.getOptionType();
	    			killingQueue.remove(a);
	    			this.blackboard.killingQueue.remove(a.getId());
	    			killAgent(a);
	    			numAgentsIntroduced++;
	    			addAgent(killedAgentType,opType);// add a new agent to replace the killed one
	    			
	    		}
	    		else{}
	    	}	
			
		}else{
			//agent kill them self by checking if itself is in killingQueue.
		}
		
		
    }
    
    
    
    
    
    /**
     * Find and decompose a task, this is used for TOimplementation=1
     * @return <code>task</code> A new decomposed task
     * @throws IOException 
     */
    private Task findNewTask() {
    	
    	
    	if (tick==1){
    		//post the inital number of tasks onto blackboard at tick 1
    			AddInitialTask();
    	}
    	
//    	//Get task's information from configuration files
//    	Properties configTask = new Properties();
//    	
//    	try {
////			configTask.load(this.getClass().getClassLoader().getResourceAsStream("config_task"+taskOpenness+".properties"));
//    		configTask.load(this.getClass().getClassLoader().getResourceAsStream("config_task"+"_TO"+taskOpenness+this.hardTaskPerString+this.averageTaskPerString));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//    	
    	
    	
    	
    	
//    	if(taskOpenness == 0) {
//    		try {
//				configTask.load(this.getClass().getClassLoader().getResourceAsStream("config_task0.0.properties"));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//    	}
//    	else if(taskOpenness == 0.2) {
//    		try {
//				configTask.load(this.getClass().getClassLoader().getResourceAsStream("config_task0.2.properties"));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//    	}
//    	else if(taskOpenness == 0.5) {
//    		try {
//				configTask.load(this.getClass().getClassLoader().getResourceAsStream("config_task0.5.properties"));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//    	}
//    	else if(taskOpenness == 1) {
//    		try {
//				configTask.load(this.getClass().getClassLoader().getResourceAsStream("config_task1.0.properties"));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//    	}
    	
    	String t = Integer.toString(tick+InitalTaskNumber);
    	/*
    	 * trying to grab the task from config files starting from InitalTaskNumer+1.
    	 */
    	if(tick <= totalTick) {
//    	if((taskOpenness == 0 || taskOpenness == 0.2 || taskOpenness == 0.5 || taskOpenness == 1) && tick <= totalTick) {
    		String task_type = configTask.getProperty(t); 	
        	print("******* task type "+task_type);
        	
        	// get the task specifics from the configuration file
//        	Properties configFile = new Properties();
//        	try {
//				configFile.load(this.getClass().getClassLoader().getResourceAsStream("config_types.properties"));
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
        	
        	
        	String num_subtasks = configFile.getProperty("Num_Subtasks"+task_type);
        	String subtasks  = configFile.getProperty("Subtasks"+task_type);	
        	String Num_Agents = configFile.getProperty("Num_Agents"+task_type);
        	String Quality = configFile.getProperty("Quality"+task_type);  	       	
        	//print("s="+num_subtasks+" g="+subtasks);
        	
    		Task task = new Task();
    		task.setId(++lastTaskId);
    		task.setType(Integer.parseInt(task_type));
    		task.setNumSubtasks(Integer.parseInt(num_subtasks));
    		//print("******** num subtasks "+Integer.parseInt(num_subtasks));
    		print(" New task id "+task.getId());
    		print("num_subtasks"+num_subtasks+" subtasks"+subtasks+" Num_Agents"+Num_Agents+" Quality"+Quality);
    	
    		// decompose task into subtasks
    		//**************************************************
    		ArrayList<Integer> subtasksList = new ArrayList<Integer>();
    		String [] subtasksString = subtasks.split(",");
    		//change it to integer array 
    		for(int i=0; i<subtasksString.length; i++) {
    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
    			subtasksList.add(Integer.parseInt(subtasksString[i]));
    		}
    		
    		ArrayList<Integer> Num_AgentsList = new ArrayList<Integer>();
    		String[] Num_AgentsString = Num_Agents.split(",");
    		for(int i=0; i<Num_AgentsString.length; i++) {
    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
    			Num_AgentsList.add(Integer.parseInt(Num_AgentsString[i]));
    		}
    		   		
    		ArrayList<Double> QualityList = new ArrayList<Double>();
    		String[] QualityString = Quality.split(",");
    		for(int i=0; i<QualityString.length; i++) {
    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
    			QualityList.add(Double.parseDouble(QualityString[i]));
    		}
 
    		task.decompose(subtasksList,Num_AgentsList,QualityList);
    		print(" finished decomposing" );
    		return task;
    		}
    	return null;
    }
    
    
    
  
    /**
     * initialize the task pool of 100 tasks 
     */
    private void InitializeTaskPool() {
		// TODO Auto-generated method stub
    	// choose 100 numbers from[0,15503] to put into the pool, they are the index of the tasks in TaskList
    	for (int i = 0; i < taskPoolSize; i++) {
			int num = MainAgent.uniform.nextIntFromTo(0,totalNumOfTasks-1);
			while (this.uniqueTaskInPool.contains(num)){
				num = MainAgent.uniform.nextIntFromTo(0,totalNumOfTasks-1);
			}
			tasksInPool.add(num);
			this.uniqueTaskInPool.add(num);
		}
    	
    	
    	/**
    	 * choose 20 tasks from the taskInPool list to put into sampledTaskPool
    	 */
    	sampleTasks();
    	
    			
    		
	}
    
    /**
     * initialize the task pool of 100 tasks 
     */
    private void InitializeTaskPoolNoSampling() {
    	// choose 100 numbers from[0,15503] to put into the pool, they are the index of the tasks in TaskList
    	for (int i = 0; i < taskPoolSize; i++) {
//			int num = random.nextInt(totalNumOfTasks);
			int num = MainAgent.uniform.nextIntFromTo(0,totalNumOfTasks-1);
			while (this.uniqueTaskInPool.contains(num)){
				num = MainAgent.uniform.nextIntFromTo(0,totalNumOfTasks-1);
			}
			tasksInPool.add(num);
			this.uniqueTaskInPool.add(num);
		}
    	
    	
    	/**
    	 * choose 20 tasks from the taskInPool list to put into sampledTaskPool
    	 */
//    	sampleTasks();
    	
    			
    		
	}
    
    
    
    
    
    
    
    
    
    
    
    
    
    

	/**
     * Find and decompose initial number of tasks, this method is called at the first tick to introduce certain number of tasks as the initial tasks on blackboard for agent to bid 
     * this method add the tasks to returnedMessage list, to be treated as returened tasks and to be posted together with the first new task
     * @return <code>task</code> A new decomposed task
     * @throws IOException 
     */
    private void AddInitialTask() {
//    	//Get task's information from configuration files

    	
    	for (int j=1;j<=InitalTaskNumber;j++){
    		String task_type = configTask.getProperty(Integer.toString(j)); 	

        	String num_subtasks = configFile.getProperty("Num_Subtasks"+task_type);
        	String subtasks  = configFile.getProperty("Subtasks"+task_type);	
        	String Num_Agents = configFile.getProperty("Num_Agents"+task_type);
        	String Quality = configFile.getProperty("Quality"+task_type);  	       	
        	//print("s="+num_subtasks+" g="+subtasks);
        	
    		Task task = new Task();
    		task.setId(++lastTaskId);
    		task.setType(Integer.parseInt(task_type));
    		task.setNumSubtasks(Integer.parseInt(num_subtasks));
    		//print("******** num subtasks "+Integer.parseInt(num_subtasks));
    		print(" New task id "+task.getId());
    		print("num_subtasks"+num_subtasks+" subtasks"+subtasks+" Num_Agents"+Num_Agents+" Quality"+Quality);
    	
    		// decompose task into subtasks
    		//**************************************************
    		ArrayList<Integer> subtasksList = new ArrayList<Integer>();
    		String [] subtasksString = subtasks.split(",");
    		//change it to integer array 
    		for(int i=0; i<subtasksString.length; i++) {
    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
    			subtasksList.add(Integer.parseInt(subtasksString[i]));
    		}
    		
    		ArrayList<Integer> Num_AgentsList = new ArrayList<Integer>();
    		String[] Num_AgentsString = Num_Agents.split(",");
    		for(int i=0; i<Num_AgentsString.length; i++) {
    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
    			Num_AgentsList.add(Integer.parseInt(Num_AgentsString[i]));
    		}
    		   		
    		ArrayList<Double> QualityList = new ArrayList<Double>();
    		String[] QualityString = Quality.split(",");
    		for(int i=0; i<QualityString.length; i++) {
    			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
    			QualityList.add(Double.parseDouble(QualityString[i]));
    		}
 
    		task.decompose(subtasksList,Num_AgentsList,QualityList);
//    		print(" finished decomposing" );
    		
    		
    		/*
    		 * package the task into blackboardMessage and add them to returnedMessages, the mainAgent will treat  these messages as returned messages and post them together with the new message at tick 1.
    		 */
    		returnedMessages.add(new BlackboardMessage(task, task.getSubtasks(),blackboard));
    		
    		}
    	print(" finished adding inital tasks to returnedMessages list, wait to be posted on blackboard together with the first new task" );
    }
    
    
    /**
     * post the new found and decomposed task message along with old returned messages to blackboard
     * @param <code>task</code> the new found and decomposed task 
     */
    private void postMessages(Task task){
    	ArrayList<BlackboardMessage> messagesToPost = new ArrayList<BlackboardMessage>();
    	//adding returned messages
    	if (!returnedMessages.isEmpty()){
    		//ArrayList<BlackboardMessage> messagesToPost = new ArrayList<BlackboardMessage>(returnedMessages);
    		messagesToPost.addAll(returnedMessages);
    		for (BlackboardMessage Msg: returnedMessages){
    			print(String.format("Post Returned task -- task %d to blackboard ", Msg.getTask().getId()));
    			
    		}
    	}
    	
    	
    	//adding newly founded decomposed task
    	messagesToPost.add(new BlackboardMessage(task, task.getSubtasks(),blackboard));
    	returnedMessages.clear();
    	print(String.format("Post new task -- task %d to blackboard ", task.getId()));
    	blackboard.post(messagesToPost); 
    	
    	
    	//adding this new task to SharedEncounteredTaskSet
    	blackboard.getSharedEncounteredTaskSet().add(task.getId());
    	blackboard.getSharedNewTaskSet().add(task.getType());
    	//update the sharedTaskOpenness
    	blackboard.updateSharedTaskOpenness();
    	
    }
    
//    @Watch (watcheeClassName = "AdhocCollaboration.Blackboard",
//			watcheeFieldNames = "allHasResponsed", //this is triggered when all agents has submitted the bid
//			whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
    public void holdAuction(){  	
    	blackboard.auction();
    	//blackboard.resetBlackboard();
    }
    
    /**
     * 
     * @return <code>true</code> if the MainAgent should kill one agent at current tick, <code>false</code> otherwise
     */
//    private boolean timeToKill(){
////    	if (this.tickToKill2==-1){
////    		if(tick > 0 && (tick % tickToKill == 0)){
////        		return true;
////        	}
////    	}
////    	else{
////    		if(tick > 0 && ((tick % tickToKill == 0) || (tick % tickToKill2 == 0))){
////        		return true;
////        	}
////    	}
//    	
//    	if(tick > 0 && (tick % tickToKill == 0)){
//    		return true;
//    	}
//    	
//    	return false;
//    }
//    
//    private boolean timeToKill2(){
//    	if (this.tickToKill2==-1){
//    		return false;
//	}
//	else{
//		if(tick > 0 &&  (tick % tickToKill2 == 0)){
//    		return true;
//    	}
//	}
//    	return false;
//    	
//    }
    
    /**
     * 
     * @param <code>agent</code> an agent to be removed
     */
    private void killAgent(Agent agent){
		//blackboard.RemoveFromAgentMap(agent.getId());
    	blackboard.updateTotalLostCap(agent.getIndividualLearnedCap());
    	blackboard.getKilledAgentSet().add(agent.getId());
    	//blackboard.updateSharedAgentOpenness();
		blackboard.getAgentList().remove(agent);
		context.remove(agent);


		print(String.format("Kill agent %d", agent.getId()));
		print("Killed agents are "+ blackboard.getKilledAgentSet());
	}
    
//    @Watch (watcheeClassName = "AdhocCollaboration.Blackboard",
//			watcheeFieldNames = "newAssignments",
//			whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
    public void receiveReturnedMessage(){
    	print(String.format("MainAgent is receiving returned messages"));
    	returnedMessages.addAll(blackboard.getReturnedMessages());
    }
    
    /**
     * Introduce a new agent into environment, we add the agent that is the same type of the killed agent(initial type when it was created)
     */
   public void addAgent(int agentType,int opType){
   
		agentQualityMax = 1;
		Agent newAgent = new Agent(++lastAgentId, blackboard, agentType, agentQualityMax,initalCapNum);
		newAgent.setOptionType(opType);
		
		newAgent.outputAgentCapToFile();
		context.add(newAgent);
		blackboard.addAgent(newAgent);
		blackboard.AddToAgentMap(newAgent.getId(), newAgent );
		blackboard.setNumUnassignedAgents(blackboard.getNumUnassignedAgents()+1);
		blackboard.setNumAgentsIntroduced(numAgentsIntroduced);
		print(String.format("Add new agent %d ", newAgent.getId()));
    }
	


	/**
     * 
     * @return A randomly selected agent
     */
	private Agent getOneAgent(){
		IndexedIterable<Agent> agents = context.getObjects(Agent.class);
//		int randomNum = random.nextInt(agents.size());
		int randomNum = MainAgent.uniform.nextIntFromTo(0,agents.size()-1);
	
		//print("Do not Kill agents "+NonKillAgentId1+" "+NonKillAgentId2+" "+NonKillAgentId3 );
		//do not choose agent which agent number is "NonKillAgentId1" to kill
		int choosenAgentId=agents.get(randomNum).getId();
		while (choosenAgentId==NonKillAgentId1 || choosenAgentId==NonKillAgentId2 ||choosenAgentId==NonKillAgentId3){
			print("Do not Kill this agent !!!!!!!!!!!!!!!!!!!!!!!");
//			randomNum = random.nextInt(agents.size());
			randomNum = MainAgent.uniform.nextIntFromTo(0,agents.size()-1);
			
			choosenAgentId=agents.get(randomNum).getId();
		}
		
		
		Agent selectedAgent = agents.get(randomNum);
		print(String.format("Get random agent %d to kill", selectedAgent.getId()));
		return selectedAgent;
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
    
    /**
     * 
     * @param <code>context</code> the context contains all agents
     */
    public void setContext(Context context){
    	this.context = context;
    }
    
    
    public Context getContext(){
    	return this.context;
    }

    /**
     * 
     * @return agent count
     */
	public int getAgentCount() {
		return agentCount;
	}
	
	/**
	 * 
	 */
	public int getUnfinishedTasks(){
		return returnedMessages.size();
	}
	
	/**
	 * 
	 * @return tick
	 */
    public static int getTick() {
		return tick;
	}

	public static int getNonKillAgentId1() {
		return NonKillAgentId1;
	}

	public static int getNonKillAgentId2() {
		return NonKillAgentId2;
	}

	public static int getNonKillAgentId3() {
		return NonKillAgentId3;
	}
	
//	public int getTickToKill2(){
//		int actualKill=(int) (Math.floor((double)totalTick/tickToKill));
//		System.out.println("actualKill round down = "+actualKill);
//		double needToKill2= (agentCount*agentOpenness-actualKill);
//		System.out.println("needToKill2 = "+needToKill2);
//		if (needToKill2==0){
//			tickToKill2=-1;//if tickToKill2=-1 then do not do extra kill
//		}
//		else{
//			tickToKill2=(int) Math.floor((double)totalTick/needToKill2);
//		}
//		
//		return tickToKill2;
//		
//	}
//	
	
//	public void checkTimeToKill1(){
//    	if(timeToKill()){
//    		System.out.println(String.format("tick %d, time to kill!", tick));
//    		Agent selectedAgent = getOneAgent();
//        	while(killingQueue.contains(selectedAgent)){
//        		selectedAgent = getOneAgent();
//        	}
//        	killingQueue.add(selectedAgent);
//        	System.out.println(String.format("Add agent %d into killing queue", selectedAgent.getId()));
//    	}
//	}
//	
//	public void checkTimeToKill2(){
//    	if(timeToKill2()){
//    		System.out.println(String.format("tick %d, time to kill!", tick));
//    		Agent selectedAgent = getOneAgent();
//        	while(killingQueue.contains(selectedAgent)){
//        		selectedAgent = getOneAgent();
//        	}
//        	killingQueue.add(selectedAgent);
//        	System.out.println(String.format("Add agent %d into killing queue", selectedAgent.getId()));
//    	}
//
//	}
	/**
	 * this method reads in config_typesRandom.properties and create all the tasks, put them into TaskList
	 */
	public static void readInFileAndCreateTasks(){
		String fileName="data/config_typesRandom.properties";
		
		File inputFile = new File(fileName);
		Scanner s = null;
		try {
			s = new Scanner(inputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	
		
		int lastTaskId=0;
		
		String task_type = "";
		String num_subtasks = "";
    	String subtasks = "" ;
    	String Num_Agents = "";
    	String Quality = ""  ;
    	
    	
//    	String num_subtasks = configFile.getProperty("Num_Subtasks"+task_type);
//    	String subtasks  = configFile.getProperty("Subtasks"+task_type);	
//    	String Num_Agents = configFile.getProperty("Num_Agents"+task_type);
//    	String Quality = configFile.getProperty("Quality"+task_type);  	 
		
		while (s.hasNext()){
			
			String line=s.nextLine().replaceAll("\\s","");
			
			if (line.isEmpty()){
				
				/*
				 * create new task
				 */
				lastTaskId++;
				
				createTaskFromFile(lastTaskId, task_type,num_subtasks,subtasks, Num_Agents,Quality );
				
				
				
				
				/**
				 * clear the strings
				 */
				task_type="";
				num_subtasks="";
		    	 subtasks ="";
		    	 Num_Agents="";
		    	 Quality ="" ;
			}else{
				String data[] = null;
//				System.out.println(line);
				
				data=line.split("=");
				if (data[0].equals("task_Type")){
					task_type=data[1];
//					System.out.println("read task type=" +task_type);
				}else if(data[0].equals("Num_Subtasks"+task_type)){
					num_subtasks=data[1];
//					System.out.println("read num_subtasks = "+num_subtasks);
				}else if(data[0].equals("Subtasks"+task_type)){
					subtasks=data[1];
//					System.out.println("read subtasks = "+subtasks);
				}else if(data[0].equals("Num_Agents"+task_type)){
					Num_Agents=data[1];
//					System.out.println("read Num_Agents = "+Num_Agents);
				}else if(data[0].equals("Quality"+task_type)){
					Quality=data[1];
//					System.out.println("read Quality = "+Quality);
				}
					
					
				}
			}
		/**
		 * create the last task
		 */
		lastTaskId++;
		
		createTaskFromFile(lastTaskId, task_type,num_subtasks,subtasks, Num_Agents,Quality );
//			System.out.println("Read in Tasks from file done. total tasks = "+TaskList.size());
	
		s.close();
	
	}
	
	
	public static void createTaskFromFile(int lastTaskId,String task_type, String num_subtasks,String subtasks,String Num_Agents,String Quality ){
		Task task = new Task();
		task.setId(lastTaskId);
		task.setType(Integer.parseInt(task_type));
		task.setNumSubtasks(Integer.parseInt(num_subtasks));
		//print("******** num subtasks "+Integer.parseInt(num_subtasks));
//		System.out.println(" New task id "+task.getId());
//		System.out.println("num_subtasks"+num_subtasks+" subtasks"+subtasks+" Num_Agents"+Num_Agents+" Quality"+Quality);
	
		// decompose task into subtasks
		//**************************************************
		ArrayList<Integer> subtasksList = new ArrayList<Integer>();
		String [] subtasksString = subtasks.split(",");
		//change it to integer array 
		for(int i=0; i<subtasksString.length; i++) {
			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
			subtasksList.add(Integer.parseInt(subtasksString[i]));
		}
		
		ArrayList<Integer> Num_AgentsList = new ArrayList<Integer>();
		String[] Num_AgentsString = Num_Agents.split(",");
		for(int i=0; i<Num_AgentsString.length; i++) {
			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
			Num_AgentsList.add(Integer.parseInt(Num_AgentsString[i]));
		}
		   		
		ArrayList<Double> QualityList = new ArrayList<Double>();
		String[] QualityString = Quality.split(",");
		for(int i=0; i<QualityString.length; i++) {
			//print("*********** subtask "+Integer.parseInt(subtasksString[i]));
			QualityList.add(Double.parseDouble(QualityString[i]));
		}

		task.decompose(subtasksList,Num_AgentsList,QualityList);	
		
		TaskList.add(task);
	}
	

	public int getTicksToFinish() {
		return ticksToFinish;
	}

	public void setTicksToFinish(int ticksToFinish) {
		this.ticksToFinish = ticksToFinish;
	}

	public String getTaskDistrubution() {
		return TaskDistrubution;
	}

	public void setTaskDistrubution(String taskDistrubution) {
		TaskDistrubution = taskDistrubution;
	}

	public String getHardTaskPerString() {
		return hardTaskPerString;
	}

	public void setHardTaskPerString(String hardTaskPerString) {
		this.hardTaskPerString = hardTaskPerString;
	}

	public String getAverageTaskPerString() {
		return averageTaskPerString;
	}

	public void setAverageTaskPerString(String averageTaskPerString) {
		this.averageTaskPerString = averageTaskPerString;
	}

	public int getUsingAgentDistrubution() {
		return UsingAgentDistrubution;
	}

	public void setUsingAgentDistrubution(int usingAgentDistrubution) {
		UsingAgentDistrubution = usingAgentDistrubution;
	}

	public int getUsingTaskDistrubution() {
		return UsingTaskDistrubution;
	}

	public void setUsingTaskDistrubution(int usingTaskDistrubution) {
		UsingTaskDistrubution = usingTaskDistrubution;
	}

	public Uniform getUniform() {
		return uniform;
	}

	public void setUniform(Uniform uniform) {
		this.uniform = uniform;
	}
	
	
}
