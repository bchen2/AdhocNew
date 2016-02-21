/**
 * 
 */
package AdhocCollaboration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import cern.colt.Arrays;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.engine.watcher.Watch;
import repast.simphony.engine.watcher.WatcherTriggerSchedule;
import repast.simphony.parameter.Parameters;

/**
 * @author Xi Chen
 * @author Bin Chen
 *
 */
public class Agent {
	private Parameters params;
	private int initalCapNum;// Number of capabilities agents have when created
	private int agentId;
	private int option;// option=99 then different agent can use different task
						// selection strategies in one simulation
	int optionType = 0;// when option=99, this optionType determines
								// which task selection strategy this agent will
								// use.
	private int agentType;/* 1 - expert, 2 - average ,3-novice */
	private int finishedSubtasks;
	private int numFailedSubtasks;
	private double w;
	private double maxQuality;
	private boolean isBusy;
	// private double agentOpenness;
	private Blackboard bb;
	private Random random;
	private BlackboardMessage taskToBidMessage;// stores the task that this
												// agent is about to bid at this
												// tick
	private BlackboardMessage taskToBeExcutedMessage; // a blackboard message
														// contains assigned
														// task to be executed
	private ArrayList<SubTask> subtaskToBeExecuted; //
	private ArrayList<Double> qualityList; // quality for this agent(i.e.
											// qualityList[0] is the quality for
											// cap 1)
	private HashMap<Task, ArrayList<SubTask>> myTaskMap; // task, list of
															// subtasks
	private int ticksToFinishRunning;
	private HashMap<Integer, HashMap<Integer, Integer>> blackList;// agentId,
																	// subtaskId,
																	// failedTimes
	ArrayList<BlackboardMessage> historyMessage;
	BlackboardMessage temporaryMessage;// contains one last participated message
										// which learning gain and reward has
										// not been calculated yet
	private double agentOpennessPerception;
	private HashMap<Integer, Double> agentOpennessPerceptionMap;// maps tick
																// number to
																// agentOpennessPerception.
	private ArrayList<Double> agentOpennessPerceptionList;// record the
															// agentOpennessPerception.
	private double taskOpennessPerception;
	private Set<Integer> newTaskSet;// store task_type
	private Set<Integer> encounteredTaskSet;// store task_id
	private HashMap<Integer, Double> taskOpennessPerceptionMap;// maps tick
																// number to
																// taskOpennessPerception.
	private ArrayList<Double> taskOpennessPerceptionList;// record the
															// taskOpennessPerception.
	private Set<Integer> knownAgentSet;// record all the agents that this agent
										// has cooperated in a task before
	private int agentOpennessOption;// --1. Agent knows only its collaborator.
									// --2.Agents share info and every agent has
									// the same agent openness. --3.Given the
									// exact agent openness in the beginning
	private int taskOpennessOption;// --1.Agent calculates task openness based
									// on the tasks it has seen by itself.
									// --2.Agents share info and every agent has
									// the same task openness --3. Given the
									// exact task openness in the beginning
	private double individualLearnedCap;
	private int taskAssignmentAtOneTick;// records agent's assigned task at this
										// tick
	private ArrayList<Double> capDiff;// records the difference of
										// cap-qualityThreshold of a task
	private int numTaskInvolved;// records the total number of tasks this agent
								// get involved in. will be updated when agents
								// read assignment
	private int numSubtasksAssignedAtOneTick = 0;// records the numSubtasks
													// assigned at each tick
	private boolean neverAssignedJob;
	private boolean selectedForCurrentBid = false;// when agent bids for a task,
													// if he gets selected
													// (ranked high) then it
													// will be true. Notice,
													// even true, the task still
													// can fail to auction off
													// due to not enough
													// capabile agent
	private int numBidsSubmitted = 0;// record the total number of bid this
										// agent submitted
	private int numbidsSubmittedAndWon = 0;// record the total number of this
											// agents's bid get selected

	private int TaskIdForLearningGain = 0;// When agent update learning gain,
											// this records the taskId that this
											// agent gained learning from
	private double TaskLearningGain = 0;// the learning gain from the task it
										// finishes
	private int TaskTobidAtOneTick = 0;// records the task that the agent bid at
										// that tick, if agent did not bid, then
										// it will be 0;
	private int TaskTypebidAtOneTick = 0;
	private int NumberOfAvailableTasksAtOneTick;// reords number of Available
												// Tasks at this tick when agent
												// check the blackboard to check
												// all the tasks,when agent is
												// executing task, this will be
												// -1;
	private double taskReward = 0;// record the task reward for the task this
									// agent is bidding
	private double selfGainAtOneTick = 0;
	private double observationGainAtOneTick = 0;
	public HashSet<Integer> ObservedSubtaskSet = new HashSet<Integer>();// records
																		// the
																		// subtask
																		// it
																		// learned
																		// from
																		// observation,
																		// later
																		// we
																		// check
																		// if
																		// these
																		// subtask
																		// get
																		// assigned
	public int ObservedCapUsedCount = 0;
	public int ObservedCapUsedCountAtOneTick = 0;

	/*
	 * code for potential Utility decay it records the number of times that an
	 * agent bid for a task and failed, when agent compute the potential utility
	 * of the avaliable tasks to determine which task to bid, it will look at
	 * this map and find out if the task he has bid before and failded, if it is
	 * the case , then the total potential utility for this task will be
	 * discounted
	 */
	private HashMap<Integer, Integer> biddingFailRecordMap; // map taskType to
															// number of times
															// that bid fails
	double decayFactor = 0.1;

	/*
	 * this is used for new Paper refer to "ad-hocUtility_09_23CommentedSohAdam.
	 * Agents want to maximize its reward, we need to keep track of the rewards
	 */
	private double reward = 0;
	private double rewardAtCurrentTick = 0;

	/**
	 * this records the message that the agent has made a bid on, we look into
	 * this list to find the tasks that are in nearest k neighbors of the task
	 * that the agent is currently evaluating and calculate P_wb and P_off
	 */
	private ArrayList<BlackboardMessage> agentbidMessageList = new ArrayList<BlackboardMessage>();
	private int k = 5; // nearest k neighbors of the tasks
	private BlackboardMessage messageToRecord;// record the blackboard message
												// that this agent bid for,
												// later on update the RejectA,
												// RejectB on it

	/*
	 * 
	 * this is used for new Paper refer to
	 * "adhoc-new U_sove2015_07_15CommentedSoh" document. When agents select an
	 * task to bid, it evaluate the task first, using the EU_solve, it can
	 * estimate the expected utility of solving this task EU_solve(T) =
	 * P_wb*P_off|wb*(1-P_failure(T))*R_t
	 */

	private double P_wb = 0.5;
	private double P_off_wb = 0.5;
	// *********for F_RA***********
	private double F_RA = 0;
	public int Delta = 10; // moving window size, we let the Delta be ticks
	public int index = 0; // use this to indicate the index of the rolling
							// window list to update

	/*
	 * RejectA(because itself is not competitive, there are better agents for
	 * the subtask, because this agent bid for this task, it must qualify for at
	 * least one of the subtasks, if it did not get selected, then it is because
	 * of the competitors.) NOTE: this RejectA, then it does not mean the Task
	 * is not auctioned off, the task can still be auctioned off, it just this
	 * agent did not get selected for any subtasks. Hence we need to check
	 * RjectA after every auction to update RejectA, not just when the Task is
	 * not auctioned off.
	 */
	public int RejectA = 0;// update this using RejectAList after each auction
	public int bidsSubmitted = 0; // count every tick the number of bids this
									// agent submitted. Every tick agent can
									// submitted 1 tick, if it is busy or not
									// qualified at all , then the bidSubmitted
									// could be 0.
	public int[] RejectAList = new int[Delta];// updated in
												// updateRejectionReason() when
												// agent readAssignment(). keep
												// an moving window of RejectA,
												// we sum over the last Delta
												// ticks to get RejectA
	public int[] bidsSumbittedList = new int[Delta];
	public int RejectARollingSum = 0;// sum over the RejectAlist
	public int bidsSubmittedRollingSum = 0;// sum over the bidsSubmittedList

	// *********for f ************
	public double f = 0;// f is updated in selectTask(), if agent is busy, then
						// it won't select task, then f won't be updated
	public int qualThreshAbove = 0;// keeps the count, it is updated in
									// FindU_learn
	public int numSubtaskEvaluated = 0;// updated in findU_learn
	public int[] qualThreshAboveList = new int[Delta];// Initialize an list of
														// size Delta to do the
														// rolling window of
														// ticks
	public int[] numSubtaskEvaluatedList = new int[Delta];
	// ********* for g ************
	public double g = 0;
	public int numAgentsAssigned = 0;
	public int numAgentsRequired = 0;
	public int[] numAgentsAssignedList = new int[Delta];
	public int[] numAgentsRequiredList = new int[Delta];

	// ********* for F_RB**********
	public double F_RB = 0;
	public int RejectB = 0;// RejectB (not enough qualified agents bid for this
							// task, not because of this agent itself), This
							// means Task did not get auctioned off, we can
							// update RejectB when the task is not auctioned
							// off.
	public int bidsSubmittedAndWon = 0;
	public int[] RejectBList = new int[Delta];// updated in
												// updateRejectionReason() when
												// agent readAssignment(). keep
												// an moving window of RejectB,
												// we sum over the last Delta
												// bids to get RejectB
	public int[] bidsSubmittedAndWonList = new int[Delta]; // #BidsSubmitted|_wb
	public int RejectBRollingSum = 0;// sum of the RejectBList
	public int bidSubmittedAndWonRollingSum = 0;// Sum of the
												// bidsSubmittedAndWonList

	public Agent(int parmAgentId, Blackboard b, int type,
			double agentQualityMax, int initalCapNummber) {
		// print("******adding a new agent now");
		initalCapNum = initalCapNummber;
		agentId = parmAgentId;
		agentType = type;
		finishedSubtasks = 0;
		maxQuality = agentQualityMax;
		bb = b;
		random = new Random();
		taskToBeExcutedMessage = null;
		subtaskToBeExecuted = new ArrayList<SubTask>();
		blackList = new HashMap<Integer, HashMap<Integer, Integer>>();
		taskToBidMessage = null;
		ticksToFinishRunning = 0;
		numFailedSubtasks = 0;
		qualityList = new ArrayList<Double>();
		myTaskMap = new HashMap<Task, ArrayList<SubTask>>();// records the task
															// and subtasks this
															// agent has
		historyMessage = new ArrayList<BlackboardMessage>();
		temporaryMessage = null; // contains one last participated message which
									// learning gain and reward has not been
									// calculated yet
		for (int i = 0; i < 20; i++) {
			qualityList.add(i, 0.0);
		}

		biddingFailRecordMap = new HashMap<Integer, Integer>();

		if (b.getUsingAgentDistrubution() == 1) {
			/*
			 * AgentQualityHelper This object helps generate quality/capability
			 * for this agent This object takes initialCapNum and type
			 * (expert=1, Average=2, Novice=3) and gives back agent quality of
			 * the required type
			 */
			AgentQualityHelper agentType = new AgentQualityHelper();

			// This will contain the Initial Non zero Quality of this agent.
			double[] agentQualityList = new double[initalCapNum];

			// type (Expert, Average or Novice Agents)

			if (type == 1) // Get Expert Agent Quality
			{
				agentQualityList = agentType.GetExpertAgent(initalCapNum);
			} else if (type == 2) { // Get Average Agent Quality
				agentQualityList = agentType.GetAverageAgent(initalCapNum);
			} else { // Get Novice Agent Quality
				agentQualityList = agentType.GetNoviceAgent(initalCapNum);
			}

			for (int i = 0; i < initalCapNum; i++) {
				int index = random.nextInt(20);
				while (qualityList.get(index) > 0) {
					index = random.nextInt(20);
				}
				// Set agent quality, using the quality array received from the
				// helper class.
				qualityList.set(index, agentQualityList[i]);
			}

			print("@@@@@@@@@@@@Agent " + agentId + " Type: "
					+ agentType.CheckType(agentQualityList) + "cap is "
					+ qualityList);
		} else {// generate random agents
			for (int i = 0; i < initalCapNum; i++) {
				int index = random.nextInt(20);
				while (qualityList.get(index) > 0) {
					index = random.nextInt(20);
				}
				qualityList.set(index, random.nextDouble() * maxQuality);
			}
			print("@@@@@@@@@@@@Agent " + agentId + " No Agent Type, OpType "
					+ optionType + " cap is " + qualityList);
		}

		params = RunEnvironment.getInstance().getParameters();
		option = (Integer) params.getValue("option"); // agents has different
														// task selection
														// strategies
		agentOpennessPerception = 0;
		agentOpennessPerceptionMap = new HashMap<Integer, Double>();
		agentOpennessPerceptionMap.put(1, 0.0);// Initialize
		agentOpennessPerceptionList = new ArrayList<Double>(0);
		taskOpennessPerception = 1;
		taskOpennessPerceptionMap = new HashMap<Integer, Double>();
		taskOpennessPerceptionMap.put(1, 1.0);// Initialize
		taskOpennessPerceptionList = new ArrayList<Double>(1);
		knownAgentSet = new HashSet<Integer>(getId());// add itself into this
														// set
		agentOpennessOption = (Integer) params.getValue("agentOpennessOption");
		taskOpennessOption = (Integer) params.getValue("taskOpennessOption");
		newTaskSet = new HashSet<Integer>();
		encounteredTaskSet = new HashSet<Integer>();
		individualLearnedCap = 0;
		taskAssignmentAtOneTick = 0;// 0 means no task assignment, task number
									// starts at 1
		capDiff = new ArrayList<Double>();
		numTaskInvolved = 0;
		neverAssignedJob = true;

		numSubtasksAssignedAtOneTick = 0;
		// learningType = 0;
		// w = 0.5;
		// agentOpenness = (Double) params.getValue("agent_openness");
		// learningSubtask = new SubTask();
		// subtaskList = new ArrayList<SubTask>();
		// historyLearningUtility = new
		// ArrayList<HashMap<Integer,HashMap<SubTask,HashMap<Integer,Double>>>>();
		// localSatMap = new HashMap<Task, HashMap<SubTask,Double>>();
		// numEncountersTask = new HashMap<Task,Integer>();
		// taskUtility = new HashMap<Task,Double>();
		// taskTypeUtility = new HashMap<Integer,Double>();

		// Capability id have one to one correspondence to subtask id. Subtasks
		// id start from 1.
		// WARNING!: from 1 to 20, not 0 to 19
		// bb.setAgentOpenness(agentId,agentOpenness);
	}

	public void agentSelectTask() {
		this.TaskIdForLearningGain = 0;
		this.TaskLearningGain = 0;
		setSelectedForCurrentBid(false);
		this.setRewardAtCurrentTick(0);
		// this.setNumAgentsRequired(0);
		// this.setNumAgentsAssigned(0);
		// this.setTaskReward(0);

		setSelfGainAtOneTick(0);
		setObservationGainAtOneTick(0);

		if (option == 13) {
			// we set these values to be 0, if agent has a task when
			// readAssignment(), or evaluated task when selectTask(), then these
			// value will be changed, then at the end of the tick, f,g will be
			// updated
			this.qualThreshAbove = 0;
			this.numSubtaskEvaluated = 0;// reset these to to 0, if agent is
											// busy and did not select task, we
											// know it from this 0 value and
											// updatef() can simply record an 0.
			this.numAgentsAssigned = 0;
			this.numAgentsRequired = 0;
			this.bidsSubmitted = 0;
			this.RejectA = 0;
			this.bidsSubmittedAndWon = 0;
			this.RejectB = 0;
			this.index = MainAgent.tick % this.Delta;
		}

		selectTask();
		// bb.addNumAgentHasResponsed();//when all agent has Responded, then it
		// triggers the auction to start

		// this.outputToFile();

	}

	/**
	 * 
	 */
	// @Watch (watcheeClassName = "AdhocCollaboration.Blackboard",
	// watcheeFieldNames = "newMsg",//when main agent post a task, will set this
	// to be true
	// whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	public void run() {
		this.TaskIdForLearningGain = 0;
		this.TaskLearningGain = 0;
		setSelectedForCurrentBid(false);
		this.setRewardAtCurrentTick(0);
		// this.setNumAgentsRequired(0);
		// this.setNumAgentsAssigned(0);
		// this.setTaskReward(0);

		setSelfGainAtOneTick(0);
		setObservationGainAtOneTick(0);

		if (option == 13) {
			// we set these values to be 0, if agent has a task when
			// readAssignment(), or evaluated task when selectTask(), then these
			// value will be changed, then at the end of the tick, f,g will be
			// updated
			this.qualThreshAbove = 0;
			this.numSubtaskEvaluated = 0;// reset these to to 0, if agent is
											// busy and did not select task, we
											// know it from this 0 value and
											// updatef() can simply record an 0.
			this.numAgentsAssigned = 0;
			this.numAgentsRequired = 0;
			this.bidsSubmitted = 0;
			this.RejectA = 0;
			this.bidsSubmittedAndWon = 0;
			this.RejectB = 0;
			this.index = MainAgent.tick % this.Delta;
		}

		if (subtaskToBeExecuted.size() > 0) {

			this.numSubtasksAssignedAtOneTick = 0;
			this.TaskTobidAtOneTick = 0;
			this.TaskTypebidAtOneTick = 0;
			this.NumberOfAvailableTasksAtOneTick = -1;// agent is excecting
														// task, set this to be
														// -1;
			bb.addNumAgentHasResponsed();
			executeSubtasks();
			isBusy = false;

			// this.outputToFile();

			/**
			 * check finished tasks and update rewards and learning gain
			 */
			if (temporaryMessage != null) {// check last tick finished task to
											// calculate learning gain

				// this.isBusy=false;
				checkFinishedMessage();// this checks if the task is failed or
										// not and calls updateCapbilities
			} else {
				/* no finished task, do not need to update capbilites */

			}

		} else {
			// if (option==13){
			// //update g here before it start selecting tasks, so it can use
			// the new g.
			//
			// updateg();
			// }
			selectTask();
			bb.addNumAgentHasResponsed();// when all agent has Responded, then
											// it triggers the auction to start
		}

		// after selectTask(), the auction results is ready, hence update the
		// following prameters
		if (option == 13) {
			// update these value, so agent can use it for selectTask for next
			// tick
			updatef();
			updateg();
			updateF_RA();
			updateF_RB();
		}

		// this.outputToFile();

	}

	public void agentExecuteSubtasks_updateCap_leave() {
		if (subtaskToBeExecuted.size() > 0) {

			this.numSubtasksAssignedAtOneTick = 0;
			this.TaskTobidAtOneTick = 0;
			this.TaskTypebidAtOneTick = 0;
//			this.NumberOfAvailableTasksAtOneTick = -1;// agent is excecting
														// task, set this to be
														// -1;
//			bb.addNumAgentHasResponsed();
			executeSubtasks();
			isBusy = false;


			/**
			 * check finished tasks and update rewards and learning gain
			 */
			if (temporaryMessage != null) {// check last tick finished task to
											// calculate learning gain

				// this.isBusy=false;
				checkFinishedMessage();// this checks if the task is failed or
										// not and calls updateCapbilities
			} else {
				/* no finished task, do not need to update capbilites */

			}

		} else {
		}

		// after selectTask(), the auction results is ready, hence update the
		// following prameters
		if (option == 13) {
			// update these value, so agent can use it for selectTask for next
			// tick
			updatef();
			updateg();
			updateF_RA();
			updateF_RB();
		}
		
		
		this.addToKillingQueue();
		
	
		this.outputToFile();
		
		
		
	}

	private void updateF_RB() {

		// if (this.bidsSubmittedAndWon==0) {
		//
		// //simply record 0, this needs to be done because we record these
		// values for every tick, even agent does not get assigned task,we still
		// record it
		// this.bidsSubmittedAndWonList[index]=0;
		// this.RejectBList[index]=0;
		// }else{
		// this.bidsSubmittedAndWonList[index]=this.bidsSubmittedAndWon;
		// this.RejectBList[index]=this.RejectB;
		// }

		// we also update it here becase when agent did not bid due to its busy,
		// we still needs to update the lists
		this.bidsSubmittedAndWonList[index] = this.bidsSubmittedAndWon;
		this.RejectBList[index] = this.RejectB;

		// calculate the f using the rolling window
		int bidsSubmittedAndWonSum = 0;
		int RejectBSum = 0;
		for (int num : bidsSubmittedAndWonList) {
			bidsSubmittedAndWonSum += num;
		}
		this.bidSubmittedAndWonRollingSum = bidsSubmittedAndWonSum;
		for (int num : RejectBList) {
			RejectBSum += num;
		}
		this.RejectBRollingSum = RejectBSum;

		if (bidsSubmittedAndWonSum == 0) {
			this.F_RB = 0;
		} else {
			this.F_RB = (double) RejectBSum / bidsSubmittedAndWonSum;
		}

		print("#######Tick " + MainAgent.tick + " agent " + this.agentId
				+ " F_RB = " + this.F_RB + " :RejectBList[" + index + "]="
				+ this.RejectB + " RejectBList=" + Arrays.toString(RejectBList)
				+ " bidsSubmittedAndWonList[" + index + "]="
				+ this.bidsSubmittedAndWon + " bidsSubmittedAndWonList="
				+ Arrays.toString(bidsSubmittedAndWonList));

	}

	private void updateF_RA() {

		// if (this.bidsSubmitted==0) {//agent did not bid task
		//
		// //simply record 0, this needs to be done because we record these
		// values for every tick, even agent does not get assigned task,we still
		// record it
		// this.bidsSumbittedList[index]=0;
		// this.RejectAList[index]=0;
		// }else{
		// this.bidsSumbittedList[index]=this.bidsSubmitted;
		// this.RejectAList[index]=this.RejectA;
		// }

		// we also update it here becase when agent did not bid due to its busy,
		// we still needs to update the lists
		this.bidsSumbittedList[index] = this.bidsSubmitted;
		this.RejectAList[index] = this.RejectA;

		// calculate the f using the rolling window
		int bidsSubmittedSum = 0;
		int RejectASum = 0;
		for (int num : bidsSumbittedList) {
			bidsSubmittedSum += num;
		}
		this.bidsSubmittedRollingSum = bidsSubmittedSum;
		for (int num : RejectAList) {
			RejectASum += num;
		}
		this.RejectARollingSum = RejectASum;

		if (bidsSubmittedSum == 0) {
			this.F_RA = 0;
		} else {
			this.F_RA = (double) RejectASum / bidsSubmittedSum;
		}

		print("#######Tick " + MainAgent.tick + " agent " + this.agentId
				+ " F_RA = " + this.F_RA + " :RejectAList[" + index + "]="
				+ this.RejectA + " RejectAList=" + Arrays.toString(RejectAList)
				+ " bidsSumbittedList[" + index + "]=" + this.bidsSubmitted
				+ " bidsSumbittedList=" + Arrays.toString(bidsSumbittedList));

	}

	public void outputToFile() {

		/**
		 * output into file
		 */
		if (OutputClass.agentOutput) {
			try {
				if (OutputClass.agentOutputWithCap) {
					MainAgent.writer.write(this.getAgentOutputWithCap() + "\n");
				} else {
					MainAgent.writer.write(this.getAgentOutput() + "\n");
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public void outputAgentCapToFile() {

		/**
		 * output into file
		 */
		if (OutputClass.agentCap) {
			try {
				MainAgent.agentCapWriter.write(this.getAgentCapOutput() + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	public void outputAgentUsolveUlearnToFile(double EUlearn, double EUsolve,
			int taskId, int taskType) {
		if (OutputClass.agentUsolveUlearn) {
			String output = String.format("%d,%d,%d,%d,%f,%f", this.getId(),
					taskId, taskType, MainAgent.tick, EUsolve, EUlearn);
			try {
				MainAgent.agentUsolveUlearnWriter.write(output + "\n");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public String getRejectAListString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int a : this.RejectAList) {
			sb.append(a);
			sb.append(" ");
		}
		sb.append("]");

		return sb.toString();
	}

	public String getBidsSubmittedListString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int a : this.bidsSumbittedList) {
			sb.append(a);
			sb.append(" ");
		}
		sb.append("]");

		return sb.toString();
	}

	public String getRejectBListString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int a : this.RejectBList) {
			sb.append(a);
			sb.append(" ");
		}
		sb.append("]");

		return sb.toString();
	}

	public String getBidSubmittedAndWonListString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int a : this.bidsSubmittedAndWonList) {
			sb.append(a);
			sb.append(" ");
		}
		sb.append("]");

		return sb.toString();
	}

	/**
	 * Select task to bid base on the total difference between each capabilit's
	 * quality and each subtask's threshold
	 */
	private void selectTask() {
		ArrayList<BlackboardMessage> taskList = bb.getAllMessages();// get all
																	// the tasks
																	// posted on
																	// blackboard
		this.NumberOfAvailableTasksAtOneTick = taskList.size();
		double maxPotentialUtility = 0.0;
		BlackboardMessage maxPotentialUtilityMessage = null;

		// updating newTaskSet and encounteredTaskSet
		// for(BlackboardMessage bm : taskList){
		// this.newTaskSet.add(bm.getTask().getType());
		// this.encounteredTaskSet.add(bm.getTask().getId());
		// this.bb.getSharedEncounteredTaskSet().add(bm.getTask().getId());
		// }

		// update Openness
		// this.updateOpenness();

		// print("AO = "+this.agentOpennessPerception);
		// print("TO = "+this.taskOpennessPerception);

		// Choose task by Calculating potentialUtility accounting to different
		// task selection strategies

		for (BlackboardMessage bm : taskList) {
			if (!bm.isAssigned()) {

				double potentialUtility = 0.0;

				if (option == 1) {// Strategy 1 choose the task it is most
									// qualified for
					potentialUtility = ComputePotentialUtilityStrategy1(bm);

				} else if (option == 2) {// Strategy 2 Max absolute difference
											// it is the same as U_observe, but
											// we did not just call
											// findU_observe because we don't
											// want to count #of qualified
											// agents for subtasks in U_observe
					potentialUtility = ComputePotentialUtilityStrategy2(bm);
				}

				/**
				 * count number of qualified agents for subtasks in U_learn only
				 */
				else if (option == 3) {// Strategy 3 Max 1 difference + Max
										// absolute negative difference (
										// U=U_learn)
					potentialUtility = ComputePotentialUtilityStrategy3(bm);
				} else if (option == 4) {// Strategy41 this is the same as
											// strategy3 , we set W_L=1, W_S=0,
											// so we have U=U_learn only.
					potentialUtility = ComputePotentialUtilityStrategy41(bm);
				} else if (option == 5) {// Strategy42 we set W_L=0, W_S=1, so
											// we have U=U_solve only. we did
											// not call U_solve, rewrite it
											// again to count number of
											// qualified agents for each
											// subtasks;
					potentialUtility = ComputePotentialUtilityStrategy42(bm);
				}

				/**
				 * for option 6-12 we count number of qualified agents for
				 * subtasks in U_learn only
				 */

				else if (option == 6) {// Strategy43 we set W_L=0.5, W_S=0.5, so
										// we have U=0.5*U_learn+0.5*U_solve .
					potentialUtility = ComputePotentialUtilityStrategy43(bm);
				} else if (option == 7) {// Strategy44 we set W_L=0.25,
											// W_S=0.75, so we have
											// U=0.25*U_learn+0.75*U_solve .
					potentialUtility = ComputePotentialUtilityStrategy44(bm);
				} else if (option == 8) {// Strategy45 we set W_L=0.75,
											// W_S=0.25, so we have
											// U=0.75*U_learn+0.25*U_solve .
					potentialUtility = ComputePotentialUtilityStrategy45(bm);
				} else if (option == 9) {// Strategy46 we set W_L=1, W_S=1, so
											// we have U=U_learn+U_solve .
					potentialUtility = ComputePotentialUtilityStrategy46(bm);
				} else if (option == 10) {// Strategy5 we set W_L=AO, W_S=1-AO,
											// so we have
											// U=AO*U_learn+(1-AO)*U_solve .
					potentialUtility = ComputePotentialUtilityStrategy5(bm);
				} else if (option == 11) {// Strategy6 we set W_L=1-TO, W_S=TO,
											// so we have
											// U=(1-TO)*U_learn+TO*U_solve .
					potentialUtility = ComputePotentialUtilityStrategy6(bm);
				} else if (option == 12) {// Strategy7 Total Potential Utilities
											// With AO and TO:
											// AO/(AO+TO)*U_learn+
											// TO/(AO+TO)*U_solve
					potentialUtility = ComputePotentialUtilityStrategy7(bm);
				} else if (option == 13) {// new Strategy using EU_Solve , not
											// using this in new paper
					potentialUtility = ComputePotentialUtilityStrategy8(bm);
				} else if (option == 14) {// new Strategy,
											// TU=P_wb*P_off(LU+E(R_t)), where
											// E(R_t) is expected rewards for
											// solving the task,
											// LU=(U_doing+U_oberve)
					potentialUtility = ComputePotentialUtilityStrategy9(bm);
				} else if (option == 15) {// new Strategy compare with option
											// 14, this only maximize E(R_t);
					potentialUtility = ComputePotentialUtilityStrategy10(bm);
				} else if (option == 16) {// new Strategy, TU=LU+E(R_t) did not
											// consider P_wb and P_off
					potentialUtility = ComputePotentialUtilityStrategy11(bm);
				} else if (option == 17) {// new Strategy, TU=P_wb*P_off*E(R_t)
					potentialUtility = ComputePotentialUtilityStrategy12(bm);
				} else if (option == 99) {// in this case agents in the same
											// eviroemnt can have mixed task
											// selection strategies
					if (this.optionType == 14) {
						potentialUtility = ComputePotentialUtilityStrategy9(bm);
					} else if (this.optionType == 15) {
						potentialUtility = ComputePotentialUtilityStrategy10(bm);
					} else if (this.optionType == 16) {
						potentialUtility = ComputePotentialUtilityStrategy11(bm);
					} else if (this.optionType == 17) {
						potentialUtility = ComputePotentialUtilityStrategy12(bm);
					} else {
						System.err
								.println("agent does not have specified task selection stategy!!!");
						potentialUtility = 0;
					}

				}

				// do not use decay here
				// potentialUtility= addDecay(potentialUtility, bm);

				// print("Task "+bm.getTask().getId()
				// +" potential = "+potentialUtility);

				// choose the one that has maxPotentialUtility
				if (potentialUtility > maxPotentialUtility) {
					/**
					 * if an agent does not bid for an task, we set the
					 * potentialUtility of that task to be negative infinity, if
					 * the agent does not bid for every task then
					 * potentialUtiltiy for every task will be negative infinity
					 * and will not satisify this if condition ,there for
					 * maxPotentialUtilityMessage will be null, that is no bid
					 * for any task
					 */
					maxPotentialUtility = potentialUtility;
					maxPotentialUtilityMessage = bm;
				}
			}
		}

		// agent bid for the task
		if (maxPotentialUtilityMessage != null) {
			// add agent to this message's agentBiddingList
			// print("Agent "+agentId+" capbility = "+this.qualityList);
			// print("Agent "+agentId+" maxPotential = "+maxPotentialUtility);
			print(String.format("Agent %d bids for task %d ", agentId,
					maxPotentialUtilityMessage.getTask().getId()));

			// print("Agent "+agentId+" capDiff for the last task= "+this.capDiff
			// +"\n");

			maxPotentialUtilityMessage.addAgentToBiddingList(this);
			this.TaskTobidAtOneTick = maxPotentialUtilityMessage.getTask()
					.getId();
			this.TaskTypebidAtOneTick = maxPotentialUtilityMessage.getTask()
					.getType();
			taskToBidMessage = maxPotentialUtilityMessage;

			// update BidsSubmitted
			this.bidsSubmitted++;
			this.numBidsSubmitted++;

			// print("Agent "+this.agentId+" bid on "+maxPotentialUtilityMessage.getTask().getId());
		} else {// agent do not bid for any task
		// print("Agent "+agentId+" capbility = "+this.qualityList);
		// print("Agent "+agentId+" maxPotential = "+maxPotentialUtility);
			print(String.format("Agent %d bids for No task", agentId));
			// print("Agent "+agentId+" capDiff for the last task = "+this.capDiff+"\n");
			this.TaskTobidAtOneTick = 0;
			this.TaskTypebidAtOneTick = 0;
			this.bidsSubmitted = 0;

		}

		// update BidsSubmittedList
		this.bidsSumbittedList[index] = bidsSubmitted;

	}

	/**
	 * this method returns a string consists of task details, as well as the
	 * task assginment
	 * 
	 * @return
	 */
	public String getTaskDetailAndAssignment() {

		if (taskToBidMessage != null) {
			String Line = "";
			for (SubTask s : taskToBidMessage.getSubtasks()) {
				Line += "[Id " + s.getId() + "|" + s.getNumAgents() + "|"
						+ s.getQuality() + "] ";
			}

			Line += "@@@" + taskToBidMessage.getAssignment();
			// print("Subtasks :"+Line);
			return Line;
		} else {
			return "Did not bid";
		}

	}

	/**
	 * f is updated every tick after the agent finished evaluating tasks, if
	 * agent is busy, it won't select task then this won't be updated
	 */
	private void updatef() {

		if (this.numSubtaskEvaluated == 0) {// agent did not select task, due to
											// its busy

			// simply record 0, this needs to be done because we record these
			// values for every tick, even agent does not select task,we still
			// record it
			this.qualThreshAboveList[index] = 0;
			this.numSubtaskEvaluatedList[index] = 0;
		} else {
			this.qualThreshAboveList[index] = this.qualThreshAbove;
			this.numSubtaskEvaluatedList[index] = this.numSubtaskEvaluated;
		}
		// calculate the f using the rolling window
		int qualThreshAboveSum = 0;
		int numSubTaskEvaluatedSum = 0;
		for (int num : this.qualThreshAboveList) {
			qualThreshAboveSum += num;
		}
		for (int num : this.numSubtaskEvaluatedList) {
			numSubTaskEvaluatedSum += num;
		}

		if (numSubTaskEvaluatedSum == 0) {
			this.f = 0;
		} else {
			this.f = (double) qualThreshAboveSum / numSubTaskEvaluatedSum;
		}

		// print("#######Tick "+MainAgent.tick +
		// " agent "+this.agentId+" f = "+this.f+" :qualThreshabove["+index+"]="+this.qualThreshAbove+" qualThreshaboveList= "+Arrays.toString(this.qualThreshAboveList)
		// +
		// " numSubtaskEvaluated["+index+"]="+this.numSubtaskEvaluated+" numSubtaskEvaluatedList="+Arrays.toString(numSubtaskEvaluatedList));
	}

	private void updateg() {

		if (this.numAgentsAssigned == 0) {// agent did not get assigned task

			// simply record 0, this needs to be done because we record these
			// values for every tick, even agent does not get assigned task,we
			// still record it
			this.numAgentsAssignedList[index] = 0;
			this.numAgentsRequiredList[index] = 0;
		} else {
			this.numAgentsAssignedList[index] = this.numAgentsAssigned;
			this.numAgentsRequiredList[index] = this.numAgentsRequired;
		}
		// calculate the f using the rolling window
		int numAgentsAssignedSum = 0;
		int numAgentsRequiredSum = 0;
		for (int num : this.numAgentsAssignedList) {
			numAgentsAssignedSum += num;
		}
		for (int num : this.numAgentsRequiredList) {
			numAgentsRequiredSum += num;
		}

		if (numAgentsRequiredSum == 0) {
			this.g = 0;
		} else {
			this.g = (double) numAgentsAssignedSum / numAgentsRequiredSum;
		}

		// print("#######Tick "+MainAgent.tick +
		// " agent "+this.agentId+" g = "+this.g+" :numAgentsAssignedList["+index+"]="+this.numAgentsAssigned+" numAgentsAssignedList="+Arrays.toString(numAgentsAssignedList)
		// +" numAgentsRequiredList["+index+"]="+this.numAgentsRequired+" numAgentsRequiredList="+Arrays.toString(numAgentsRequiredList));

	}

	/**
	 * 
	 * @param bm
	 * @return
	 */
	private double ComputePotentialUtilityStrategy1(BlackboardMessage bm) {
		double potentialUtility = 0.0;
		for (SubTask subtask : bm.getSubtasks()) {
			if (qualityList.get(subtask.getId() - 1) > subtask.getQuality()) {

				/** counting # of qualified agents for subtask */
				subtask.addQualfiedAgentsCount();

				potentialUtility += (qualityList.get(subtask.getId() - 1) - subtask
						.getQuality());
			}
		}
		return potentialUtility;
	}

	/**
	 * Strategy 2 Max absoulute negative difference (U_observe)
	 * 
	 * @param bm
	 * @return
	 */
	private double ComputePotentialUtilityStrategy2(BlackboardMessage bm) {
		// it is the same as U_observe, but we did not just call findU_observe
		// because we don't want to count #of qualified agents for subtasks in
		// U_observe

		capDiff.clear();
		double potentialUtility = 0.0;

		double maxQualDiff = -1;
		for (int i = 0; i < bm.getSubtasks().size(); i++) {
			double QualDiff = qualityList
					.get(bm.getSubtasks().get(i).getId() - 1)
					- bm.getSubtasks().get(i).getQuality();

			capDiff.add(QualDiff);
			if (QualDiff > 0) {

				/** counting # of qualified agents for subtask */
				bm.getSubtasks().get(i).addQualfiedAgentsCount();

				if (QualDiff > maxQualDiff) {// find maxQualDiff
					maxQualDiff = QualDiff;
				}
			}
		}
		if (maxQualDiff < 0) {
			// if no capability is qualified, then do not bid.
			potentialUtility = Double.NEGATIVE_INFINITY;
		} else {// has at least one capability meet the requirement, adding the
				// absolute value of the unqualified ones
			int NumSubtaskObsered = 0;
			for (double diff : capDiff) {
				if (diff < 0) {
					potentialUtility += Math.abs(diff);
					NumSubtaskObsered++;
				}
			}

			potentialUtility = (double) potentialUtility / NumSubtaskObsered;// normalize
																				// it
		}
		// print("agent "+ this.agentId+ "potentialUtility for task "+
		// bm.getTask().getId()+ "=  "+ potentialUtility);
		// print("Agent "+agentId+" capDiff for task "+bm.getTask().getId()+" = "+this.capDiff);
		return potentialUtility;
	}

	private double findU_observe(BlackboardMessage bm) {
		capDiff.clear();
		double potentialUtility = 0.0;

		double maxQualDiff = -1;
		for (int i = 0; i < bm.getSubtasks().size(); i++) {
			double QualDiff = qualityList
					.get(bm.getSubtasks().get(i).getId() - 1)
					- bm.getSubtasks().get(i).getQuality();

			capDiff.add(QualDiff);
			if (QualDiff > 0) {

				if (QualDiff > maxQualDiff) {// find maxQualDiff
					maxQualDiff = QualDiff;
				}
			}
		}
		if (maxQualDiff < 0) {
			// if no capability is qualified, then do not bid.
			potentialUtility = Double.NEGATIVE_INFINITY;
		} else {// has at least one capability meet the requirement, adding the
				// absolute value of the unqualified ones
			int NumSubtaskObsered = 0;
			for (double diff : capDiff) {
				if (diff < 0) {
					potentialUtility += Math.abs(diff);
					NumSubtaskObsered++;
				}
			}

			potentialUtility = (double) potentialUtility / NumSubtaskObsered;// normalize
																				// it
		}
		// print("agent "+ this.agentId+ "potentialUtility for task "+
		// bm.getTask().getId()+ "=  "+ potentialUtility);
		// print("Agent "+agentId+" capDiff for task "+bm.getTask().getId()+" = "+this.capDiff);
		return potentialUtility;
	}

	/** find U_learn, U_learn= 0.5U_doing+0.5U_observe */
	private double ComputePotentialUtilityStrategy3(BlackboardMessage bm) {
		return findU_learn(bm);
	}

	/** find U_learn, U_learn= 0.5U_doing+0.5U_observe */
	public double findU_learn(BlackboardMessage bm) {
		/** finding U_doing */
		capDiff.clear();
		double potentialUtility = 0.0;
		int tauJ = -1;
		// find tauJ such that qualCap-requiedCap>0 and the difference is the
		// max difference
		double maxQualDiff = -1;
		for (int i = 0; i < bm.getSubtasks().size(); i++) {
			double QualDiff = qualityList
					.get(bm.getSubtasks().get(i).getId() - 1)
					- bm.getSubtasks().get(i).getQuality();
			capDiff.add(QualDiff);

			// updating the count for numSubtaskEvaluated
			this.numSubtaskEvaluated++;

			if (QualDiff > 0) {

				// this agent is qualified for this subtask, update the count
				// for qualThreshAbove
				this.qualThreshAbove++;

				/** count number of qualified Agents for subtask */
				bm.getSubtasks().get(i).addQualfiedAgentsCount();

				// potentialUtility += (qualityList.get(subtask.getId()-1) -
				// subtask.getQuality());
				if (QualDiff > maxQualDiff) {// find maxQualDiff
					maxQualDiff = QualDiff;
					tauJ = i;
				}
			}
		}
		if (tauJ != -1) {// if found such tauJ

			// print("Task "+bm.getTask().getId()+" U_doing = "+ maxQualDiff);

			/** finding U_observe */
			// subtaskAll.remove(bm.getSubtasks().get(tauJ));
			potentialUtility += 0.5 * maxQualDiff;/*
												 * add U_doing to the
												 * potentialUtility
												 */

			int numSubtaskObserved = 0;
			double potentialUtilityFromObservation = 0;
			for (double diff : capDiff) {
				// note : agent's qualityList index starts with 0, subtaskId
				// starts with 1
				if (diff < 0) {// add up the absolute value of difference of the
								// unqualified capability and the requirement
					potentialUtilityFromObservation += Math.abs((diff));/*
																		 * add
																		 * U_observe
																		 * to
																		 * the
																		 * potentialUtility
																		 */
					numSubtaskObserved++;
				}
			}

			if (potentialUtilityFromObservation != 0) {
				potentialUtility = potentialUtility
						+ 0.5
						* (potentialUtilityFromObservation / numSubtaskObserved);
			}// else then there is no subtask to observe

			// print("Task "+bm.getTask().getId()+" U_observe= "+
			// (potentialUtility-maxQualDiff));
		} else {// if no qualified capability then do not bid
		// print("Task "+bm.getTask().getId()+" U_doing = -Infinity");
		// print("Task "+bm.getTask().getId()+" U_observe = -Infinity");
			potentialUtility = Double.NEGATIVE_INFINITY;
		}

		// print("agent "+ this.agentId+ "potentialUtility for task "+
		// bm.getTask().getId()+ "=  "+ potentialUtility);
		// print("Agent "+agentId+" capDiff for task "+bm.getTask().getId()+" = "+this.capDiff);
		// print("agent "+ this.agentId+ "U_learn for task ="+potentialUtility);
		return potentialUtility;
	}

	private double findU_solve(BlackboardMessage bm) {
		/** find qualThresh_jmax **/
		capDiff.clear();
		double U_solve = 0.0;
		int tauJ = -1;
		// find tauJ such that qualCap-requiedCap>0 and the difference is the
		// max difference
		double maxQualDiff = -1;
		for (int i = 0; i < bm.getSubtasks().size(); i++) {
			double QualDiff = qualityList
					.get(bm.getSubtasks().get(i).getId() - 1)
					- bm.getSubtasks().get(i).getQuality();
			capDiff.add(QualDiff);
			if (QualDiff > 0) {
				// potentialUtility += (qualityList.get(subtask.getId()-1) -
				// subtask.getQuality());
				if (QualDiff > maxQualDiff) {// find maxQualDiff
					maxQualDiff = QualDiff;
					tauJ = i;
				}
			}
		}
		if (tauJ != -1) {// if found such tauJ
			/**
			 * find qualThresh_jmax and the sum then do the division to get
			 * U_solve
			 */
			double qualThresh_jmax = maxQualDiff;
			double R_t = 1;// reward for completing the task, we set it for 1
							// for all task for now, may use different value for
							// different task later
			double sum = 0;// this is the sum of each qualThresh of a subtask
							// times its number of required agent
			for (SubTask subtask : bm.getSubtasks()) {
				sum = sum + subtask.getQuality() * subtask.getNumAgents();
				// sum=sum+subtask.getQuality();
			}
			// U_solve=(qualThresh_jmax*R_t)/(sum);
			U_solve = 5 * (qualThresh_jmax * R_t) / (sum);
		} else {// if no qualified capability then do not bid
			U_solve = Double.NEGATIVE_INFINITY;
		}

		// print("Task "+bm.getTask().getId()+" U_solve = "+U_solve);

		return U_solve;
	}

	private double findTotalPitentialUtilities(BlackboardMessage bm,
			double W_L, double W_S) {
		// W_L weight for learning
		// W_S weight for solving a task

		double U_solve = findU_solve(bm);
		double U_learn = findU_learn(bm);

		if (U_learn > 0) {
			// print("W_L= "+W_L+ "  W_L*U_learn= "+W_L*U_learn);
			bb.setNumU_learn(bb.getNumU_learn() + 1);
			bb.setU_learnTotal(bb.getU_learnTotal() + U_learn);
		}
		if (U_solve > 0) {
			// print("W_S= "+W_S+ "  W_S*U_solve= "+W_S*U_solve);
			bb.setNumU_solve(bb.getNumU_solve() + 1);
			bb.setU_solveTotal(bb.getU_solveTotal() + U_solve);
		}

		return W_L * U_learn + W_S * U_solve;
	}

	// Strategy4.1
	private double ComputePotentialUtilityStrategy41(BlackboardMessage bm) {
		// this is the same as strategy3 , we set W_L=1, W_S=0, so we have
		// U=U_learn only.
		return findTotalPitentialUtilities(bm, 1, 0);
	}

	// Strategy4.2
	private double ComputePotentialUtilityStrategy42(BlackboardMessage bm) {
		// this strategy is the same as just call U_solve but it counts number
		// of qualified agents for each subtasks
		// rewrite U_solve here, since we do not want to count number of
		// qualified agents for each subtasks in U_solve, we count it in U_learn
		/** find qualThresh_jmax **/
		capDiff.clear();
		double U_solve = 0.0;
		int tauJ = -1;
		// find tauJ such that qualCap-requiedCap>0 and the difference is the
		// max difference
		double maxQualDiff = -1;
		for (int i = 0; i < bm.getSubtasks().size(); i++) {
			double QualDiff = qualityList
					.get(bm.getSubtasks().get(i).getId() - 1)
					- bm.getSubtasks().get(i).getQuality();
			capDiff.add(QualDiff);
			if (QualDiff > 0) {
				// potentialUtility += (qualityList.get(subtask.getId()-1) -
				// subtask.getQuality());
				if (QualDiff > maxQualDiff) {// find maxQualDiff
					maxQualDiff = QualDiff;
					tauJ = i;
				}
			}
		}
		if (tauJ != -1) {// if found such tauJ
			/**
			 * find qualThresh_jmax and the sum then do the division to get
			 * U_solve
			 */
			double qualThresh_jmax = maxQualDiff;
			double R_t = 1;// reward for completing the task, we set it for 1
							// for all task for now, may use different value for
							// different task later
			double sum = 0;// this is the sum of each qualThresh of a subtask
							// times its number of required agent
			for (SubTask subtask : bm.getSubtasks()) {
				sum = sum + subtask.getQuality() * subtask.getNumAgents();
				// sum=sum+subtask.getQuality();
			}
			// U_solve=(qualThresh_jmax*R_t)/(sum);
			U_solve = 5 * (qualThresh_jmax * R_t) / (sum);
		} else {// if no qualified capability then do not bid
			U_solve = Double.NEGATIVE_INFINITY;
		}

		return U_solve;
	}

	// Strategy4.3
	private double ComputePotentialUtilityStrategy43(BlackboardMessage bm) {
		// we set W_L=0.5, W_S=0.5, so we have U=0.5*U_learn+0.5*U_solve .
		return findTotalPitentialUtilities(bm, 0.5, 0.5);
	}

	// Strategy4.4
	private double ComputePotentialUtilityStrategy44(BlackboardMessage bm) {
		// we set W_L=0.25, W_S=0.75, so we have U=0.25*U_learn+0.75*U_solve .
		return findTotalPitentialUtilities(bm, 0.25, 0.75);
	}

	// Strategy4.5
	private double ComputePotentialUtilityStrategy45(BlackboardMessage bm) {
		// we set W_L=0.75, W_S=0.25, so we have U=0.75*U_learn+0.25*U_solve .
		return findTotalPitentialUtilities(bm, 0.75, 0.25);
	}

	// Strategy4.6
	private double ComputePotentialUtilityStrategy46(BlackboardMessage bm) {
		// we set W_L=1, W_S=1, so we have U=1*U_learn+1*U_solve .
		return findTotalPitentialUtilities(bm, 1.0, 1.0);
	}

	// Total Potential Utilities With AO:
	private double ComputePotentialUtilityStrategy5(BlackboardMessage bm) {
		// we set W_L=AO, W_S=1-AO, so we have U=AO*U_learn+(1-AO)*U_solve .
		double W_S = 1 - this.agentOpennessPerception;
		// print("~~~~~~~~~~~~~~~1-AO= "+W_S);
		return findTotalPitentialUtilities(bm, this.agentOpennessPerception,
				W_S);
	}

	// Total Potential Utilities With TO:
	private double ComputePotentialUtilityStrategy6(BlackboardMessage bm) {
		// we set W_L=1-TO, W_S=TO, so we have U=(1-TO)*U_learn+TO*U_solve .
		return findTotalPitentialUtilities(bm,
				(1 - this.taskOpennessPerception), this.taskOpennessPerception);
	}

	// Total Potential Utilities With AO and TO:
	private double ComputePotentialUtilityStrategy7(BlackboardMessage bm) {
		double W_L = this.agentOpennessPerception
				/ (this.agentOpennessPerception + this.taskOpennessPerception);
		double W_S = this.taskOpennessPerception
				/ (this.agentOpennessPerception + this.taskOpennessPerception);
		return findTotalPitentialUtilities(bm, W_L, W_S);
	}

	/**
	 * this is used for new Paper. When agents select an task to bid, it
	 * evaluate the task first, using the EU_solve, it can estimate the expected
	 * utility of solving this task EU_solve(T) =
	 * P_wb*P_off|wb*(1-P_failure(T))*E(R_T)
	 */
	private double ComputePotentialUtilityStrategy8(BlackboardMessage bm) {
		double U_learn = findU_learn(bm);
		// double EReward=findEReward(bm);
		double EU_solve = findEU_solve1(bm);

		print("Agent " + this.agentId + " find EU_solve =  " + EU_solve
				+ "and U_learn= " + U_learn + " for Task "
				+ bm.getTask().getId());
		return U_learn + EU_solve;

	}

	/**
	 * newest strategy for the new paper, based on
	 * ad-hocUtility_09_23CommentedSohAdam, we did not use the
	 * ComputePotentialUtilityStrategy8
	 * 
	 * @param bm
	 * @return
	 */
	private double ComputePotentialUtilityStrategy9(BlackboardMessage bm) {

		double EU_solve = findEU_solve2(bm);// done
		double EU_doing = findEU_doing1(bm);// done
		double EU_observe = findEU_observe1(bm);// done
		// double EU_learn=0.5*EU_doing+0.5*EU_observe;
		double EU_learn = EU_doing + EU_observe;// do not use the 0.5

		outputAgentUsolveUlearnToFile(EU_learn, EU_solve, bm.getTask().getId(),
				bm.getTask().getType());

		ArrayList<BlackboardMessage> K_NeighborList = find_K_Neighbor(bm);

		double F_RA, F_RB, P_wb, P_off;
		if (K_NeighborList == null) {
			P_wb = 0.5;
			P_off = 0.5;
		} else {// Calculate them
			double sumRejectA = 0;
			double sumRejectB = 0;
			double sumBidsSubmittedAndWon = 0;
			for (BlackboardMessage message : K_NeighborList) {
				sumRejectA += message.RejectA;
				sumRejectB += message.RejectB;
				sumBidsSubmittedAndWon += message.bidSubmittedAndWon;
			}

			// NOTE: we use psoudel count here. we and 1 to sumRejectA and 2 to
			// BidsSubmitted.
			double size = K_NeighborList.size();
			double epsilonTop = 1 / (size + 1);
			double epsilonBottom = 4 / (size + 1);
			F_RA = (sumRejectA + epsilonTop)
					/ (K_NeighborList.size() + epsilonBottom);
			P_wb = 1 - F_RA;

			size = sumBidsSubmittedAndWon;
			epsilonTop = 1 / (size + 1);
			epsilonBottom = 4 / (size + 1);
			F_RB = (sumRejectB + epsilonTop)
					/ (sumBidsSubmittedAndWon + epsilonBottom);
			P_off = 1 - F_RB;

			// double a= (K_NeighborList.size()-sumRejectA);

			// print ("a="+a + " b="+sumBidsSubmittedAndWon);
		}

		/**
		 * EU(T) = P_wb*P_off|wb*(1-P_failure(T))*(EU_solve+EU_learn) we set
		 * P_failure(T)=0
		 */
		// find P_wb(T) and P_off(T), we used nearest k neighbor to find these
		// 2.
		// EU=P_wb(T) * P_off(T)*(1-P_failure(T))*(EU_learn+EU_solve)

		double EU = P_wb * P_off * 1 * (EU_solve + EU_learn);
		print("==========Agent " + this.agentId + "=============");
		print("P_wb=" + P_wb + "  P_off=" + P_off);
		print("EU_doing=" + EU_doing + "        EU_observe=" + EU_observe);
		print("EU= " + EU + "     EU_sove=" + EU_solve + "    EU_learn="
				+ EU_learn);

		return EU;

	}

	/**
	 * use ComputePotentialUtilityStrategy10 to compare with
	 * ComputePotentialUtilityStrategy9 Strategy10 only find EU_solve, which is
	 * the naive way to maximize reward, whereas Strategy9 considered learning
	 * and the probability.
	 * 
	 * @param bm
	 * @return
	 */
	private double ComputePotentialUtilityStrategy10(BlackboardMessage bm) {
		return findEU_solve2(bm);
	}

	private double ComputePotentialUtilityStrategy11(BlackboardMessage bm) {
		double EU_solve = findEU_solve2(bm);// done
		double EU_doing = findEU_doing1(bm);// done
		double EU_observe = findEU_observe1(bm);// done
		// double EU_learn=0.5*EU_doing+0.5*EU_observe;
		double EU_learn = EU_doing + EU_observe;// do not use the 0.5

		// outputAgentUsolveUlearnToFile(EU_learn,EU_solve,bm.getTask().getId(),
		// bm.getTask().getType());

		return EU_solve + EU_learn;
	}

	/**
	 * the method considers the probability and the expected utility of solving
	 * the task (Task Reward).
	 * 
	 * @param bm
	 * @return
	 */
	private double ComputePotentialUtilityStrategy12(BlackboardMessage bm) {
		double EU_solve = findEU_solve2(bm);// done
		// double EU_doing=findEU_doing1(bm);//done
		// double EU_observe=findEU_observe1(bm);//done
		// double EU_learn=EU_doing+EU_observe;// do not use the 0.5

		// outputAgentUsolveUlearnToFile(EU_learn,EU_solve,bm.getTask().getId(),
		// bm.getTask().getType());

		ArrayList<BlackboardMessage> K_NeighborList = find_K_Neighbor(bm);

		double F_RA, F_RB, P_wb, P_off;
		if (K_NeighborList == null) {
			P_wb = 0.5;
			P_off = 0.5;
		} else {// Calculate them
			double sumRejectA = 0;
			double sumRejectB = 0;
			double sumBidsSubmittedAndWon = 0;
			for (BlackboardMessage message : K_NeighborList) {
				sumRejectA += message.RejectA;
				sumRejectB += message.RejectB;
				sumBidsSubmittedAndWon += message.bidSubmittedAndWon;
			}

			// NOTE: we use psoudel count here. we and 1 to sumRejectA and 2 to
			// BidsSubmitted.
			double size = K_NeighborList.size();
			double epsilonTop = 1 / (size + 1);
			double epsilonBottom = 4 / (size + 1);
			F_RA = (sumRejectA + epsilonTop)
					/ (K_NeighborList.size() + epsilonBottom);
			P_wb = 1 - F_RA;

			size = sumBidsSubmittedAndWon;
			epsilonTop = 1 / (size + 1);
			epsilonBottom = 4 / (size + 1);
			F_RB = (sumRejectB + epsilonTop)
					/ (sumBidsSubmittedAndWon + epsilonBottom);
			P_off = 1 - F_RB;

		}

		/**
		 * EU(T) = P_wb*P_off|wb*(1-P_failure(T))*(EU_solve+EU_learn) we set
		 * P_failure(T)=0
		 */
		// find P_wb(T) and P_off(T), we used nearest k neighbor to find these
		// 2.
		// EU=P_wb(T) * P_off(T)*(1-P_failure(T))*(EU_learn+EU_solve)

		double EU = P_wb * P_off * 1 * (EU_solve);
		print("==========Agent " + this.agentId + "=============");
		print("P_wb=" + P_wb + "  P_off=" + P_off);
		// print("EU_doing="+EU_doing +"        EU_observe="+EU_observe);
		print("EU= " + EU + "     EU_sove=" + EU_solve + "    EU_learn=0");

		return EU;
	}

	private double findEU_doing(BlackboardMessage bm) {
		double eta = 0.01;
		double epsilon = 0.001;
		double EU_doing = 0.0;

		for (SubTask subtask : bm.getSubtasks()) {
			int subtaskId = subtask.getId();

			double QualDiff = this.getCapabilityQuality(subtaskId)
					- subtask.getQuality();

			if (QualDiff > 0) {
				double LearningGain = (eta / (this
						.getCapabilityQuality(subtaskId) + epsilon));// self
																		// learn
																		// getCapabilityQuality(subtaskId)=qualityList.get(subtaskId-1)
				EU_doing += QualDiff * LearningGain;
			}

		}

		return EU_doing;

	}

	/**
	 * this is used our new learning by doing gain function
	 * 
	 * @param bm
	 * @return
	 */
	private double findEU_doing1(BlackboardMessage bm) {
		double EU_doing = 0.0;

		for (SubTask subtask : bm.getSubtasks()) {
			int subtaskId = subtask.getId();

			double alphaS = MainAgent.subtaskLearningType.alphaS.get(subtaskId);
			// System.out.println("Agent"+this.agentId+ "subtask"+subtaskId +
			// "alpha="+alphaS);
			double cap = this.getCapabilityQuality(subtaskId);

			double QualDiff = cap - subtask.getQuality();
			if (QualDiff < 0) {
				QualDiff = 0;
			}

			double selfGain = alphaS * cap * (1 - cap);
			// System.out.println("rawSelfGain="+selfGain);
			selfGain = QualDiff * selfGain;

			double newQuality = (double) this.getCapabilityQuality(subtaskId)
					+ selfGain;
			if (newQuality > 1) {// max is 1
				newQuality = 1;
				selfGain = 1 - (double) this.getCapabilityQuality(subtaskId);
			}

			EU_doing += selfGain;

		}

		return EU_doing;

	}

	/**
	 * this is used our new learning by doing gain function for AAMAS2016 paper
	 * Same as findEU_doing1, except it did not apply probability
	 * 
	 * @param bm
	 * @return
	 */
	private double findEU_doing2(BlackboardMessage bm) {
		double EU_doing = 0.0;

		for (SubTask subtask : bm.getSubtasks()) {
			int subtaskId = subtask.getId();

			double alphaS = MainAgent.subtaskLearningType.alphaS.get(subtaskId);
			// System.out.println("Agent"+this.agentId+ "subtask"+subtaskId +
			// "alpha="+alphaS);
			double cap = this.getCapabilityQuality(subtaskId);

			// double QualDiff=cap- subtask.getQuality();
			// if (QualDiff<0){
			// QualDiff=0;
			// }

			double selfGain = alphaS * cap * (1 - cap);
			// System.out.println("rawSelfGain="+selfGain);
			// selfGain=QualDiff*selfGain;

			double newQuality = (double) this.getCapabilityQuality(subtaskId)
					+ selfGain;
			if (newQuality > 1) {// max is 1
				newQuality = 1;
				selfGain = 1 - (double) this.getCapabilityQuality(subtaskId);
			}

			EU_doing += selfGain;

		}

		return EU_doing;

	}

	private double findEU_observe(BlackboardMessage bm) {

		double EU_observe = 0.0;

		for (SubTask subtask : bm.getSubtasks()) {
			// find CapDiff
			int subtaskId = subtask.getId();
			double Diff = this.getCapabilityQuality(subtaskId)
					- subtask.getQuality();
			/*
			 * we define QualDiff the following way: if capbility of this
			 * subtask t - qualThresh_t<=0, then QualDiff=0, else
			 * QualDiff=capbility of this subtask t - qualThresh_t
			 */
			double CapDiff;
			if (Diff > 0) {
				CapDiff = Diff;
			} else {
				CapDiff = 0;
			}
			double qualThresh = subtask.getQuality();
			double numAgent = subtask.getNumAgents();
			double n_max = 5;

			double Ex = (qualThresh + (1 - qualThresh) * (numAgent / n_max))
					- this.getCapabilityQuality(subtaskId);
			double EGain_observe = calcuateLearningByObservationGain(Ex);

			// print("Equal="+(qualThresh+(1-qualThresh)*(numAgent/n_max))+"    myQual="+this.getCapabilityQuality(subtaskId));
			// print("EX="+Ex+ "    EGain_observe= "+EGain_observe);

			EU_observe += (1 - CapDiff) * EGain_observe;
		}

		return EU_observe;

	}

	/**
	 * in agent reasoning, agent only observe the subtask that gives the max
	 * observational gain.
	 * 
	 * @param bm
	 * @return
	 */
	private double findEU_observe1(BlackboardMessage bm) {

		double EU_observe = 0.0;
		double maxObservationGain = -1;
		int maxObservingSubtaskId = 0;
		for (SubTask subtask : bm.getSubtasks()) {
			int capId = subtask.getId();
			double alphaO = MainAgent.subtaskLearningType.alphaO.get(capId);
			double diff = subtask.getQuality()
					- this.getCapabilityQuality(capId);
			double delta = MainAgent.subtaskLearningType.delta;
			double gain;

			// System.out.println("Agent"+this.agentId+ "subtask"+capId +
			// "alphaObserve="+alphaO);

			if (diff <= 0 || diff >= delta) {
				gain = 0;
			} else {
				gain = alphaO * diff * (delta - diff);
				// apply the probability
				gain = (1 - diff) * gain;
			}

			if (gain > maxObservationGain) {
				maxObservationGain = gain;
				maxObservingSubtaskId = capId;
			}

		}

		double updatedQuality = getCapabilityQuality(maxObservingSubtaskId)
				+ maxObservationGain;
		if (updatedQuality > 1) {
			updatedQuality = 1;
			EU_observe = 1 - getCapabilityQuality(maxObservingSubtaskId);
		}

		// EU_observe+=gain;

		return EU_observe;

	}

	/**
	 * in agent reasoning, agent only observe the subtask that gives the max
	 * observational gain. same as findEU_observe1, except this one does not
	 * apply probability
	 * 
	 * @param bm
	 * @return
	 */
	private double findEU_observe2(BlackboardMessage bm) {

		double EU_observe = 0.0;
		double maxObservationGain = -1;
		int maxObservingSubtaskId = 0;
		for (SubTask subtask : bm.getSubtasks()) {
			int capId = subtask.getId();
			double alphaO = MainAgent.subtaskLearningType.alphaO.get(capId);
			double diff = subtask.getQuality()
					- this.getCapabilityQuality(capId);
			double delta = MainAgent.subtaskLearningType.delta;
			double gain;

			// System.out.println("Agent"+this.agentId+ "subtask"+capId +
			// "alphaObserve="+alphaO);

			if (diff <= 0 || diff >= delta) {
				gain = 0;
			} else {
				gain = alphaO * diff * (delta - diff);
				// apply the probability
				// gain=(1-diff)*gain;
			}

			if (gain > maxObservationGain) {
				maxObservationGain = gain;
				maxObservingSubtaskId = capId;
			}

		}

		double updatedQuality = getCapabilityQuality(maxObservingSubtaskId)
				+ maxObservationGain;
		if (updatedQuality > 1) {
			updatedQuality = 1;
			EU_observe = 1 - getCapabilityQuality(maxObservingSubtaskId);
		}

		// EU_observe+=gain;

		return EU_observe;

	}

	/**
	 * this is used for new Paper. When agents select an task to bid, it
	 * evaluate the task first, using the EU_solve, it can estimate the expected
	 * utility of solving this task EU_solve(T) =
	 * P_wb*P_off|wb*(1-P_failure(T))*R_T
	 */
	private double findEU_solve(BlackboardMessage bm) {
		// F_RA,f,g , F_RB are updated when last auction ends
		// calculate P_wb
		this.P_wb = Math.min(1, this.P_wb + this.P_wb
				* ((1 - this.F_RA) + this.f - this.g));
		// print("P_wb= "+this.P_wb+ " for Task "+bm.getTask().getId());

		// calculate P_off_wb
		this.P_off_wb = Math.min(1, this.P_off_wb + this.P_off_wb
				* ((1 - this.F_RB) + this.g));
		// print("P_off_wb= "+this.P_off_wb+ " for Task "+bm.getTask().getId());

		double EU_solve = this.P_wb * this.P_off_wb * 1
				* bm.getTask().getReward();// for now, the failure rate is 0 and
											// the R_T is 1 for every task
		// print("EU_solve= "+EU_solve+ " for Task "+bm.getTask().getId());
		return EU_solve;
	}

	/**
	 * this is the modification of findEU_solve. now we define P_wb=1-F_RA, and
	 * P_off=1-F_RB
	 * 
	 * @param bm
	 * @return
	 */
	private double findEU_solve1(BlackboardMessage bm) {
		// F_RA,f,g , F_RB are updated when last auction ends
		// calculate P_wb
		this.P_wb = 1 - this.F_RA;
		// print("P_wb= "+this.P_wb+ " for Task "+bm.getTask().getId());

		// calculate P_off_wb
		this.P_off_wb = 1 - this.F_RB;
		// print("P_off_wb= "+this.P_off_wb+ " for Task "+bm.getTask().getId());

		double EReward = findEReward(bm);

		double EU_solve = this.P_wb * this.P_off_wb * 1 * EReward;

		print("Agent " + this.agentId + " finding EU_solve  for Task "
				+ bm.getTask().getId() + " --- P_wb= " + this.P_wb
				+ " P_off_wb= " + this.P_off_wb + " EReward= " + EReward
				+ " R_t= " + bm.getTask().getReward());

		// print("EU_solve= "+EU_solve+ " for Task "+bm.getTask().getId());
		return EU_solve;
	}

	private double findEU_solve2(BlackboardMessage bm) {
		double EReward = findEReward1(bm);
		return EReward;
	}

	/**
	 * this method find the expected reward of completing this given task, by
	 * estimating the portion of reward that an agent can get
	 * 
	 * @param bm
	 * @return
	 */
	private double findEReward(BlackboardMessage bm) {
		/** find capDiff **/
		capDiff.clear();
		double EReward = 0.0;

		boolean bidTask = false;// if agent is not qualified for any subtask,
								// then bidTask will be false, the EReward will
								// be set to 0

		double sum1 = 0;// sum of QualDiff_t*qualThresh_t for all t in T

		/*
		 * we define QualDiff the following way: if capbility of this subtask t
		 * - qualThresh_t<=0, then QualDiff=0, else QualDiff=capbility of this
		 * subtask t - qualThresh_t
		 */
		for (int i = 0; i < bm.getSubtasks().size(); i++) {
			double QualDiff = qualityList
					.get(bm.getSubtasks().get(i).getId() - 1)
					- bm.getSubtasks().get(i).getQuality();

			if (QualDiff > 0) {
				bidTask = true;
				capDiff.add(QualDiff);
				sum1 += QualDiff * bm.getSubtasks().get(i).getQuality();
			} else {
				capDiff.add((double) 0);
			}
		}

		if (!bidTask) {
			EReward = 0;
			return EReward;
		} else {
			double R_t = bm.getTask().getReward();// reward for completing the
													// task
			sum1 = sum1 * R_t;

			EReward = sum1 / bm.getSum2();

			return EReward;
		}

		// print("Task "+bm.getTask().getId()+" U_solve = "+U_solve);

	}

	/**
	 * updated on 1/13/2016 this method find the expected reward of completing
	 * this given task, by estimating the portion of reward that an agent can
	 * get
	 * 
	 * @param bm
	 * @return
	 */
	private double findEReward1(BlackboardMessage bm) {
		/** find capDiff **/
		capDiff.clear();
		double EReward = 0.0;

		boolean bidTask = false;// if agent is not qualified for any subtask,
								// then bidTask will be false, the EReward will
								// be set to 0

		double sum1 = 0;

		/*
		 * we define QualDiff the following way: if capbility of this subtask t
		 * - qualThresh_t<=0, then QualDiff=0, else QualDiff=capbility of this
		 * subtask t - qualThresh_t
		 */
		for (int i = 0; i < bm.getSubtasks().size(); i++) {
			double QualDiff = qualityList
					.get(bm.getSubtasks().get(i).getId() - 1)
					- bm.getSubtasks().get(i).getQuality();

			if (QualDiff > 0) {
				bidTask = true;
				capDiff.add(QualDiff);

				sum1 += QualDiff
						* bm.getSubtasks().get(i).getQuality()
						/ (bm.getSubtasks().get(i).getNumAgents() * bm
								.getSumQuality());

				// sum1+=QualDiff*bm.getSubtasks().get(i).getQuality();
			} else {
				capDiff.add((double) 0);
			}
		}

		if (!bidTask) {
			EReward = 0;
			return EReward;
		} else {
			double R_t = bm.getTask().getReward();// reward for completing the
													// task
			EReward = sum1 * R_t;
			return EReward;
		}

		// print("Task "+bm.getTask().getId()+" U_solve = "+U_solve);

	}

	// @Watch (watcheeClassName = "AdhocCollaboration.Blackboard",
	// watcheeFieldNames = "newAssignments",//when action is done (main agent
	// has announced the action result, it sets newAssignments=true and trigger
	// agents to read assignment
	// whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	public void readAssignment() {
		this.ObservedCapUsedCountAtOneTick = 0;

		if (taskToBidMessage != null) {// if agent did not bid for task (due to
										// it is busy) then it will not read
										// assignment( this method does nothing)

			// updateBidsSubmittedAndWon();//this is done in updated
			// rejectionReason for now
			this.numAgentsRequired = taskToBidMessage.getTask()
					.getNumReqiredAgents();
			this.taskReward = taskToBidMessage.getTask().getReward();
			// read its own subtask assignment and store it in the arrayList
			// subtaskToBeExecuted
			subtaskToBeExecuted = taskToBidMessage
					.getSubtasksAssignmentByAgent(agentId);

			if (subtaskToBeExecuted.size() > 0) {// if this agent has an
													// assignment
			// System.out.println("set Agent "+this.agentId+" busy!");
				this.isBusy = true;
				this.numSubtasksAssignedAtOneTick = subtaskToBeExecuted.size();

				/**
				 * check if the learned by observation Cap ever get used to make
				 * the agent get an subtask
				 */
				// for(SubTask subtask: subtaskToBeExecuted){
				// int capId=subtask.getId();
				// if(this.ObservedSubtaskSet.contains(capId)){
				// this.ObservedCapUsedCount++;
				// this.ObservedCapUsedCountAtOneTick++;
				// // System.out.println("~~~~~~!!!!Used Observed Cap!");
				// }
				// }

				// System.err.println("Agent "+this.agentId+" Subtasks Assigned = "+this.numSubtasksAssignedAtOneTick);

				if (this.neverAssignedJob) {
					this.bb.setNumUnassignedAgents(this.bb
							.getNumUnassignedAgents() - 1);
					this.neverAssignedJob = false;
				}

				if (this.bb.getUniqueAgentSolvingTaskSet().contains(agentId)) {
					this.bb.getAgentSolvedMultipleTasksSet().add(agentId);
				}

				this.bb.getUniqueAgentSolvingTaskSet().add(agentId);

				// when agent has an assignment, if the option is 13, then it
				// will update the numRequiredAgents and numAssigendAgents,
				// then before the next tick when agent select a task, it will
				// have the g updated and use it for choosing the task
				if (option == 13) {
					// update numRequiredAgents and numAssigendAgents
					this.numAgentsRequired = taskToBidMessage.getTask()
							.getNumReqiredAgents();
					// print("%%%%%%%%%%%%%Agent "+agentId+ " Task "+
					// taskToBidMessage.getTask().getId()+
					// " numAgentRequired = "+this.numAgentsRequired);
					// get numAssignedAgents
					this.numAgentsAssigned = taskToBidMessage
							.getNumAgentsAssigned();
					// print("%%%%%%%%%%%%%Agent "+agentId+ " Task "+
					// taskToBidMessage.getTask().getId()+
					// " numAgentAssigned = "+this.numAgentsAssigned);

				}

				this.numAgentsRequired = taskToBidMessage.getTask()
						.getNumReqiredAgents();
				this.numAgentsAssigned = taskToBidMessage
						.getNumAgentsAssigned();

				// System.err.println("Agent "+this.agentId+" numAgentsAssigned = "+this.numAgentsAssigned);

				// increasing the count of numTaskInvolved
				this.numTaskInvolved++;

				taskToBeExcutedMessage = taskToBidMessage;

				this.taskAssignmentAtOneTick = taskToBeExcutedMessage.getTask()
						.getId();
				// this.taskAssignmentAtOneTick=taskToBeExcutedMessage.getTask().getType();

				// adding all the collaborators into knownAgentSet and
				// SharedKnowAgentSet
				// for (SubTask subtask: taskToBeExcutedMessage.getSubtasks()){
				// this.knownAgentSet.addAll(taskToBeExcutedMessage.getSubtasksAssignmentBySubtaskId(subtask.getId()));
				// this.bb.getSharedKnowAgentSet().addAll(taskToBeExcutedMessage.getSubtasksAssignmentBySubtaskId(subtask.getId()));
				// }

				ticksToFinishRunning = taskToBeExcutedMessage.getTask()
						.getTickToFinish();

			} else {// this agent did not have an assignment i.e. did not win
					// this task in the auction

				updateBidingFailingRecord();
				this.numSubtasksAssignedAtOneTick = 0;
				this.taskAssignmentAtOneTick = 0;// no task assignment at this
													// tick
				isBusy = false;

			}

			/**
			 * record this message (make a copy) to agentbidMessageList for
			 * finding nearest k neighbor later
			 */
			messageToRecord = new BlackboardMessage(taskToBidMessage.getTask(),
					taskToBidMessage.getSubtasks());
			agentbidMessageList.add(messageToRecord);

			/*
			 * check rejection Reason. It could be RejectA: agent itself did not
			 * get ranked high enough for the subtask, or RejectB: the agent's
			 * bid get rejected due to there are not enough qualified agent.
			 * NOTE: when task is auctioned off, RejcetA can still happen, due
			 * to the agent did not ranked high to get assignment. When agent
			 * has assignment then RejectA=0. We only check Rejection Reason
			 * when the agent did not get an assignment.
			 */
			updateRejectionReason();

			taskToBidMessage = null;
			messageToRecord = null;
		} else {// agent did not bid
			this.taskAssignmentAtOneTick = 0;// no task assignment at this tick
			this.numSubtasksAssignedAtOneTick = 0;
			this.numAgentsRequired = 0;
			this.numAgentsAssigned = 0;
			isBusy = false;
		}

	}
	
	/**
	 * if agent needs to leave, add itself into the killingQueue, then mainAgent can remove them and update the AgentList
	 */
	private void addToKillingQueue(){
		double x=Math.random();
		if (x<=this.bb.getAgentOpenness()){
			this.bb.agentKillList.add(this);
			print("agent "+this.agentId +" has been put into the agentKillList.");
			
		}
	}
	
	
	
	
	

	private void removeAgent() {

		this.outputToFile();

		/**
		 * agent exit the environment
		 */
		if (this.bb.killAgentImplementation == 2) {

			if (!this.getBusy()) {
				/*
				 * agent remove itself
				 */
				if (this.bb.killingQueue.contains(this.agentId)) {
					// System.out.println("kill agent "+this.agentId);
					int killedAgentType = this.getAgentType();
					int opType = this.optionType;
					this.bb.getKilledAgentSet().add(this.getId());
					this.bb.getAgentList().remove(this);
					this.bb.mainAgent.getContext().remove(this);

					/*
					 * call main agent to add a new one
					 */
					this.bb.mainAgent.numAgentsIntroduced++;
					this.bb.mainAgent.addAgent(killedAgentType, opType);// add a
																		// new
																		// agent
																		// to
																		// replace
																		// the
																		// killed
																		// one
				}
			}

		}

	}

	/**
	 * this method checks the subtaskSelectedAgentMap in BlackboardMessage, if
	 * the agentId is contained in it (meaning it is qualified and is selected
	 * for the subtask), then the reason is rejectB, if not then it is RejectA.
	 * This method is called only when agent does not have an assignment.
	 */
	private void updateRejectionReason() {
		boolean found = false;
		for (ArrayList<Integer> value : taskToBidMessage
				.getSubtaskSelectedAgentMap().values()) {
			// using ArrayList#contains
			if (value.contains(this.agentId)) {
				found = true;
				messageToRecord.bidSubmittedAndWon = 1;
				// this.numbidsSubmittedAndWon++;//we might have agents get
				// selected for multiple subtasks, then we added it multiple
				// times, so we do not do it here
				setSelectedForCurrentBid(true);
			}
		}

		if (found) {
			this.numbidsSubmittedAndWon++;
		} else {
			setSelectedForCurrentBid(false);
		}

		// if (found){
		// this.RejectB=1;
		//
		// }else{
		// this.RejectA=1;
		//
		// }
		// this.RejectAList[index]=RejectA;
		// this.RejectBList[index]=RejectB;

		if (!found) {
			this.RejectA = 1;
			messageToRecord.RejectA = 1;
		} else {
			if (!taskToBidMessage.auctionedOff) {
				this.RejectB = 1;
				messageToRecord.RejectB = 1;
			}
		}
		this.RejectAList[index] = RejectA;
		this.RejectBList[index] = RejectB;

	}

	private void updateBidsSubmittedAndWon() {
		boolean found = false;
		for (ArrayList<Integer> value : taskToBidMessage
				.getSubtaskSelectedAgentMap().values()) {
			// using ArrayList#contains
			if (value.contains(this.agentId)) {
				found = true;
			}
		}

		if (found) {
			this.bidsSubmittedAndWon++;
			taskToBidMessage.bidSubmittedAndWon = 1;
		} else {

		}

		this.bidsSubmittedAndWonList[index] = bidsSubmittedAndWon;

	}

	/**
	 * Execute subtasks of a task. Update local counter of finisheSubtasks
	 * Update blackboard's counter of finishedSubtasks
	 */
	private void executeSubtasks() {
		if (ticksToFinishRunning > 1) {
			ticksToFinishRunning--;
			Task task = taskToBeExcutedMessage.getTask();
			int remainingTicks = task.getTickToFinish() - ticksToFinishRunning;
			print(String.format(
					"Agent %-5d start executing subtask(s) of task%-5d  %d/%d",
					this.agentId, task.getId(), remainingTicks,
					task.getTickToFinish()));
			// for(int i = 0; i < subtaskToBeExecuted.size(); i++){
			// try{
			// // Thread.sleep(10);
			// }
			// catch(Exception e){}
			// }
		} else {
			Task task = taskToBeExcutedMessage.getTask();
			// print(String.format("Agent %d start executing subtasks of task%d",this.agentId,task.getId()));
			// print(String.format("Agent %-5d start executing subtasks of task%-5d  %d/%d",this.agentId,
			// task.getId(), task.getTickToFinish(), task.getTickToFinish()));
			int numFinishedSubtasks = 0;
			for (SubTask subtask : subtaskToBeExecuted) {
				try {
					// Thread.sleep(10);

					/**
					 * // 5% failure rate for each agent each subtask. Will be
					 * changed base on the difference between required
					 * capability and agent capability if(random.nextInt(100) <
					 * 5){ //subtask failed
					 * taskToBeExcutedMessage.setSubtaskFailed(subtask,
					 * agentId); numFailedSubtasks++;//update the total
					 * numFailedSubtasks of this particular agent }
					 * else{//subtask succeed numFinishedSubtasks++;
					 * taskToBeExcutedMessage.setSubtaskFinish(subtask); }
					 */
					numFinishedSubtasks++;
					taskToBeExcutedMessage.setSubtaskFinish(subtask);
				} catch (Exception e) {
				}
			}
			myTaskMap.put(task, subtaskToBeExecuted);// store it here for later
														// reference
			finishedSubtasks += numFinishedSubtasks;// update the total
													// finishedSubtasks of this
													// particular agent
			temporaryMessage = taskToBeExcutedMessage;
			taskToBeExcutedMessage = null;
			subtaskToBeExecuted.clear();

			bb.setNumFinishedSubtasks(numFinishedSubtasks);// update the total
															// NumFinishedSubtasks
															// on blackboard
			// isBusy = false;
			print(String.format("Agent %d finished %d subtasks of task %-4d",
					this.agentId, numFinishedSubtasks, task.getId()));
		}
	}

	/**
	 * agent calculate learning gain after task's completion(next tick) update
	 * learning by doing and learning by observation together
	 */
	private void updateCapabilities() {
		double c = 2.0;
		double eta = 0.01;
		double epsilon = 0.001;
		// learn by doing from the temporaryMessage
		Task task = temporaryMessage.getTask();

		ArrayList<SubTask> subtaskAll = new ArrayList<SubTask>(
				task.getSubtasks());
		print(this.agentId + " read assingment  "
				+ temporaryMessage.getAssignment());
		print(this.agentId + " to do subtask "
				+ temporaryMessage.getSubtasksAssignmentByAgent(agentId));
		for (SubTask subtask : temporaryMessage
				.getSubtasksAssignmentByAgent(agentId)) {
			double elearn = 1;// learn type is learn by Practice, has
								// effectiveness of 2
			int subtaskId = subtask.getId();
			int capId = subtaskId;
			double LearningGain = (eta / (this.getCapabilityQuality(subtaskId) + epsilon));// self
																							// learn
																							// getCapabilityQuality(subtaskId)=qualityList.get(subtaskId-1)
			double DeltaQuality = elearn * LearningGain; // the total quality
															// gain from
															// learning
			this.bb.updateLearnedCap(DeltaQuality);// record learned quality

			this.updateIndividualLearnedCap(DeltaQuality);// record individual
															// learned quality
			this.setSelfGainAtOneTick(DeltaQuality);
			this.TaskLearningGain = TaskLearningGain + DeltaQuality;

			double newQuality = (double) this.getCapabilityQuality(subtaskId)
					+ DeltaQuality;
			if (newQuality > 1) {// max is 1
				newQuality = 1;
			}
			print(String
					.format("Agent %d 's capability %d gained %.4f from completing subtask%d (from task%d type%d),    %.4f---> %.4f \n",
							this.getId(), capId, DeltaQuality, subtaskId,
							task.getId(), task.getType(),
							this.getCapabilityQuality(subtaskId), newQuality));
			// System.out.printf("Agent %d 's capability %d gained %.4f from completing subtask%d (from task%d),    %.4f---> %.4f \n",this.getId(),capId,DeltaQuality,subtaskId,task.getId(),this.getCapabilityQuality(subtaskId),newQuality);

			// update newQuality
			this.setCapabilityQuality(capId, newQuality);
			subtaskAll.remove(subtask);// remove the subtask executed by this
										// agent from all the subtasks of this
										// task, these remaining subtasks are
										// the ones this agent learn from
										// observing
		}

		// Calculate learning by observation gain
		for (SubTask observingSubtask : subtaskAll) {
			int capId = observingSubtask.getId();
			ArrayList<Integer> agentIdList = temporaryMessage
					.getSubtasksAssignmentBySubtaskId(observingSubtask.getId());
			double maxGain = 0;
			double gain;
			double diff;
			// if a subtask has more than one agent to do, choose the agent who
			// can give the max learning from observing gain to learn from
			for (Integer agentId : agentIdList) {
				diff = bb.getAgent(agentId).getCapabilityQuality(capId)
						- this.getCapabilityQuality(capId);
				if (diff > 0) {
					gain = calcuateLearningByObservationGain(diff);
					if (gain > maxGain) {
						maxGain = gain;
					}
				}
			}
			double updatedQuality = getCapabilityQuality(observingSubtask
					.getId()) + maxGain;
			this.bb.updateLearnedCap(maxGain);// record learned quality
			this.updateIndividualLearnedCap(maxGain);// record individual
														// learned quality
			this.setObservationGainAtOneTick(maxGain);
			this.TaskLearningGain = TaskLearningGain + maxGain;

			print(String
					.format("Agent %d gain learning %.4f utility by observating,	%.4f---> %.4f ",
							agentId, maxGain,
							getCapabilityQuality(observingSubtask.getId()),
							updatedQuality));
			this.setCapabilityQuality(capId, updatedQuality);
		}

		this.TaskIdForLearningGain = task.getType();
		print("~~~~~~~~~" + this.agentId + "    " + this.TaskIdForLearningGain);
		temporaryMessage = null;
		// print("#############oberving size: "+subtaskAll.size());
		// // learn by observing from the other subtasks other than the ones in
		// subtaskToBeExecuted
		// for (SubTask subtaskOberving:subtaskAll){
		// double elearn=0.2;//learn type is learn by Observing, has
		// effectiveness of 1
		// int subtaskId=subtaskOberving.getId();
		// print("@@@@@@subtaskId="+subtaskId);
		// int capId=subtaskId-1;
		// print("learning from "+taskToBeExecuted.getAssignment().get(subtaskId));
		// int
		// AgentToLearnFromId=taskToBeExecuted.getAssignment().get(subtaskId).get(0);//get
		// the first agent's id from the assignment for this subtask, we can
		// choose the one that have max learning gain
		// // one that have max learning gain
		// print("!!!!!!!!!AgentToLearnFromId"+AgentToLearnFromId);
		// print("==========================================");
		// //print("@@@@@@@@@@@@ai"+(double)this.getCapabilityQuality(subtaskId));
		// //get the capbility of the second agent
		// print("@@@@@@@@@@@@@@getting agent "+bb.getAgentMap().get(AgentToLearnFromId).agentId);
		// print("cap is"+bb.getAgentMap().get(AgentToLearnFromId).getCapabilityQuality(subtaskId));
		// //print("@@@@@@@@@@@@other "+(double)bb.getAgentList().get(AgentToLearnFromId-1).getCapabilityQuality(subtaskId));
		// if
		// (this.getCapabilityQuality(subtaskId)<bb.getAgentMap().get(AgentToLearnFromId).getCapabilityQuality(subtaskId)){
		// double LearningGain=(c*c - Math.pow(((
		// (double)bb.getAgentMap().get(AgentToLearnFromId).getCapabilityQuality(subtaskId))-(double)this.getCapabilityQuality(subtaskId)
		// -
		// c),2))/((double)this.getCapabilityQuality(subtaskId)+epsilon);//learn
		// by observation
		// getCapabilityQuality(subtaskId)=qualityList.get(subtaskId-1)
		// double DeltaQuality=elearn*LearningGain; //the total quality gain
		// from learning
		// double newQuality = (double)this.getCapabilityQuality(subtaskId) +
		// DeltaQuality;
		// if (newQuality>1){//max is 1
		// newQuality=1;
		// }
		// System.out.printf("Agent %d 's capability %d gained %.4f from oberving subtask%d (from task%d) from Agent%d,    %.4f---> %.4f ",this.getId(),capId,DeltaQuality,subtaskId,task.getId(),AgentToLearnFromId,this.getCapabilityQuality(subtaskId),newQuality);
		// print();
		// //update newQuality
		// this.setCapabilityQuality(capId, newQuality);
		// }
		// else
		// {
		// print("No learning!!!");
		// }
		// }
	}

	/**
	 * check finished task from last tick and update learning gain If a task
	 * failed, add agents who failed the subtask into blacklist.
	 */
	private void checkFinishedMessage() {
		if (temporaryMessage.getTaskFinishedFlag() == -1) {// task failed
			for (SubTask subtask : temporaryMessage.getSubtasks()) {
				for (Integer agentId : subtask.getFailedAgentList()) {
					if (blackList.containsKey(agentId)) {
						if (blackList.get(agentId).containsKey(subtask.getId())) {
							int times = blackList.get(agentId).get(
									subtask.getId()) + 1;
							blackList.get(agentId).put(subtask.getId(), times);
						} else {
							blackList.get(agentId).put(subtask.getId(), 1);
						}
					} else {
						blackList.put(agentId, new HashMap<Integer, Integer>());
						blackList.get(agentId).put(subtask.getId(), 1);
					}
				}
			}
		} else {// task succeed
			updateReward1();
		}
		// though task is failed, agent still gained capabilities
		updateCapabilities1();

		historyMessage.add(temporaryMessage);
		temporaryMessage = null;
	}

	private void updateReward() {
		Task task = temporaryMessage.getTask();
		// ArrayList<SubTask> subtaskAll = new
		// ArrayList<SubTask>(task.getSubtasks());

		double sum1 = 0; // this sum1 calculates summation of the qualThresh_t
							// for all t that agents get assigned
		// get subtasks that agent assigned to
		for (SubTask subtask : temporaryMessage
				.getSubtasksAssignmentByAgent(agentId)) {
			// double QualDiff=qualityList.get(subtask.getId()-1) -
			// subtask.getQuality();
			sum1 += subtask.getQuality();
		}

		double rewardForThisTask = (sum1 * task.getReward())
				/ temporaryMessage.getSum2();
		// add the reward to the total cumative reward
		this.reward += rewardForThisTask;
		this.rewardAtCurrentTick = rewardForThisTask;
		bb.updateTotalReward(rewardForThisTask);
		print("!!!!!!!!!!!!!!!!!Agent " + this.agentId + " get "
				+ rewardForThisTask + " reward for complete Task "
				+ task.getId() + "(type " + task.getType() + ")");
	}

	/**
	 * updated this reward function on 1/13/2016
	 */
	private void updateReward1() {
		Task task = temporaryMessage.getTask();
		// ArrayList<SubTask> subtaskAll = new
		// ArrayList<SubTask>(task.getSubtasks());

		double sum1 = 0;
		// get subtasks that agent assigned to
		for (SubTask subtask : temporaryMessage
				.getSubtasksAssignmentByAgent(agentId)) {
			// double QualDiff=qualityList.get(subtask.getId()-1) -
			// subtask.getQuality();

			sum1 = subtask.getQuality()
					/ (subtask.getNumAgents() * temporaryMessage
							.getSumQuality());
		}

		double rewardForThisTask = (sum1 * task.getReward());
		// add the reward to the total cumative reward
		this.reward += rewardForThisTask;
		this.rewardAtCurrentTick = rewardForThisTask;
		bb.updateTotalReward(rewardForThisTask);
		print("!!!!!!!!!!!!!!!!!Agent " + this.agentId + " get "
				+ rewardForThisTask + " reward for complete Task "
				+ task.getId() + "(type " + task.getType() + ")");
	}

	/**
	 * this is the new learning gain function we come up with for AAMAS2016
	 * paper
	 */
	public void updateCapabilities1() {

		// learn by doing from the temporaryMessage
		Task task = temporaryMessage.getTask();
		ArrayList<SubTask> subtaskAll = new ArrayList<SubTask>(
				task.getSubtasks());

		print(this.agentId + " read assingment  "
				+ temporaryMessage.getAssignment());
		print(this.agentId + " to do subtask "
				+ temporaryMessage.getSubtasksAssignmentByAgent(agentId));

		for (SubTask subtask : temporaryMessage
				.getSubtasksAssignmentByAgent(agentId)) {

			int subtaskId = subtask.getId();
			int capId = subtaskId;

			double alphaS = MainAgent.subtaskLearningType.alphaS.get(subtaskId);
			// System.out.println("Agent"+this.agentId+ "subtask"+subtaskId +
			// "alpha="+alphaS);
			double cap = this.getCapabilityQuality(subtaskId);
			double selfGain = alphaS * cap * (1 - cap);
			// System.out.println("rawSelfGain="+selfGain);

			double newQuality = (double) this.getCapabilityQuality(subtaskId)
					+ selfGain;
			if (newQuality > 1) {// max is 1
				newQuality = 1;
				selfGain = 1 - (double) this.getCapabilityQuality(subtaskId);
			}

			// System.out.println("RealSelfGain="+selfGain);

			this.bb.updateLearnedCap(selfGain);// record learned quality
			this.updateIndividualLearnedCap(selfGain);// record individual
														// learned quality
			this.setSelfGainAtOneTick(selfGain);
			this.TaskLearningGain = TaskLearningGain + selfGain;

			print(String
					.format("Agent %d 's capability %d gained %.4f from completing subtask%d (from task%d type%d),    %.4f---> %.4f \n",
							this.getId(), capId, selfGain, subtaskId,
							task.getId(), task.getType(),
							this.getCapabilityQuality(subtaskId), newQuality));

			// update newQuality
			this.setCapabilityQuality(capId, newQuality);
			subtaskAll.remove(subtask);// remove the subtask executed by this
										// agent from all the subtasks of this
										// task, these remaining subtasks are
										// the ones this agent learn from
										// observing
		}

		// Calculate learning by observation gain, agent only observe the
		// subtask that gives the max observationGain
		double maxObservationGain = -1;
		int maxObservingSubtaskId = 0;
		for (SubTask observingSubtask : subtaskAll) {
			int capId = observingSubtask.getId();

			double alphaO = MainAgent.subtaskLearningType.alphaO.get(capId);
			double diff = observingSubtask.getQuality()
					- this.getCapabilityQuality(capId);
			double delta = MainAgent.subtaskLearningType.delta;
			double gain;

			// System.out.println("Agent"+this.agentId+ "subtask"+capId +
			// "alphaObserve="+alphaO);

			if (diff <= 0 || diff >= delta) {
				gain = 0;
			} else {
				gain = alphaO * diff * (delta - diff);
			}

			if (gain > maxObservationGain) {
				maxObservationGain = gain;
				maxObservingSubtaskId = capId;
			}

		}

		if (maxObservationGain > 0) {
			double gain = maxObservationGain;

			double updatedQuality = getCapabilityQuality(maxObservingSubtaskId)
					+ gain;
			if (updatedQuality > 1) {
				updatedQuality = 1;
				gain = 1 - getCapabilityQuality(maxObservingSubtaskId);
			}
			if (gain > 0) {
				this.ObservedSubtaskSet.add(maxObservingSubtaskId);
			}

			this.bb.updateLearnedCap(gain);// record learned quality
			this.updateIndividualLearnedCap(gain);// record individual learned
													// quality
			this.setObservationGainAtOneTick(gain);
			this.TaskLearningGain = TaskLearningGain + gain;

			// System.out.println(String.format("Agent %d gain learning %.4f utility by observating,	%.4f---> %.4f ",
			// agentId, gain, getCapabilityQuality(observingSubtask.getId()),
			// updatedQuality));

			print(String
					.format("Agent %d gain learning %.4f utility by observating,	%.4f---> %.4f ",
							agentId, gain,
							getCapabilityQuality(maxObservingSubtaskId),
							updatedQuality));
			this.setCapabilityQuality(maxObservingSubtaskId, updatedQuality);

		}

		this.TaskIdForLearningGain = task.getType();
		print("~~~~~~~~~" + this.agentId + "    " + this.TaskIdForLearningGain);
		temporaryMessage = null;
	}

	/**
	 * 
	 * @param diff
	 * @return learningGain
	 */
	private double calcuateLearningByObservationGain(double diff) {
		double gain;
		if (diff > 0 && diff < 0.25) {
			gain = (-4.0 / 5.0) * Math.pow(diff, 2) + (2.0 * diff / 5.0);
		} else if (diff > 0 && diff < 1) {
			gain = (-4.0 * Math.pow(diff, 2) / 45.0) + (2.0 * diff / 45.0)
					+ (2.0 / 45.0);
		} else {
			gain = 0;
		}
		return gain;
	}

	// @Watch (watcheeClassName = "AdhocCollaboration.Task",
	// watcheeFieldNames = "isFinished",
	// whenToTrigger = WatcherTriggerSchedule.IMMEDIATE)
	// /**
	// * Calculate utility when the task is finished
	// * Decide to teach or learn
	// */
	// public void calculateUtility(){
	// for(Entry<Task, ArrayList<SubTask>> entry : myTaskMap.entrySet()){
	// Task task = entry.getKey();
	// if(task.getFinished()){
	// double localSat = 0.0;
	// double utility = 0.0;
	// if(localSatMap.containsKey(task) && !taskUtility.containsKey(task)){
	// Set<SubTask> subtaskSet = localSatMap.get(task).keySet();
	// double cost = 0;
	// for (int i = 0; i < subtaskSet.size(); i++){
	// SubTask subtask = subtaskSet.iterator().next();
	// localSat += localSatMap.get(task).get(subtask);
	// // add cost
	// }
	// utility = localSat + task.getGlobalSat() - cost;
	// taskUtility.put(task, utility);
	// }
	// }
	// }
	// }

	/**
	 * Agent make decision to teach or learn
	 */

	/**
	 * Agent decide who to learn, what to learn and how to learn
	 */

	/**
	 * Agent update its capabilities' quality after learning
	 */

	/**
	 * Agent update agentOpennessPerception
	 */
	public void updateOpenness() {
		// Updating AgentOpenness Perciption
		if (agentOpennessOption == 1) {// Agent knows only its collaborator
			Set<Integer> intersection = new HashSet<Integer>(
					this.bb.getKilledAgentSet()); // use the copy constructor
			intersection.retainAll(this.knownAgentSet);

			if (!intersection.isEmpty()) {
				this.agentOpennessPerception = (double) intersection.size()
						/ this.knownAgentSet.size();
			} else {
				this.agentOpennessPerception = 0;
			}
		} else if (agentOpennessOption == 2) {// Agents share info and every
												// agent has the same agent
												// openness
			// read it from blackboard
			// sharedAgentOpenness get updated at eachtick before main agent
			// post a new task
			this.agentOpennessPerception = this.bb.getSharedAgentOpenness();

		} else {// agentOpenness option 3--Given the exact agent openness in the
				// beginning
			this.agentOpennessPerception = this.bb.getAgentOpenness();
		}
		// record agentOpennessPerception to Map
		agentOpennessPerceptionMap.put(MainAgent.getTick(),
				agentOpennessPerception);
		agentOpennessPerceptionList.add(agentOpennessPerception);

		// Updating TaskOpenness Perciption
		if (taskOpennessOption == 1) {// Agent calculates task openness based on
										// the tasks it has seen by itself.
			// print("===========================");
			// print("agent "+this.agentId);
			this.taskOpennessPerception = (double) newTaskSet.size()
					/ this.encounteredTaskSet.size();

		} else if (taskOpennessOption == 2) {// Agents share info and every
												// agent has the same task
												// openness
			// print("~~~~~====================================2222222222");
			// read it from blackboard
			this.taskOpennessPerception = this.bb.getSharedTaskOpenness();
		} else {// taskOpenness option 3--Given the exact task openness in the
				// beginning
				// print("~~~~~====================================33333333333");
			this.taskOpennessPerception = this.bb.getTaskOpenness();
		}
		// record taskOpennessPerception to Map
		taskOpennessPerceptionMap.put(MainAgent.getTick(),
				taskOpennessPerception);
		taskOpennessPerceptionList.add(taskOpennessPerception);
	}

	/**
	 * Bidding fali record is keep in biddingFailRecordMap, it maps task_id to
	 * number of times that this agent has bid but fail to win this task
	 */
	public void updateBidingFailingRecord() {
		int task_id = taskToBidMessage.getTask().getId();
		if (this.biddingFailRecordMap.get(task_id) != null) {
			int value = this.biddingFailRecordMap.get(task_id);
			biddingFailRecordMap.put(task_id, value + 1);
			// print("agent "+this.agentId+" has failed on bidding task "+task_id+" one more time "+(value+1));
		} else {
			this.biddingFailRecordMap
					.put(taskToBidMessage.getTask().getId(), 1);
			// print("agent "+this.agentId+" first time failed on bidding task "+task_id);
		}
	}

	/**
	 * this function discount the potential utility based on the number of times
	 * this agent has failed biding this same task
	 */
	public double addDecay(double utility, BlackboardMessage bm) {
		double newUtility = 0;
		if (this.biddingFailRecordMap.get(bm.getTask().getId()) != null) {// this
																			// agent
																			// has
																			// failed
																			// biding
																			// this
																			// task
																			// before
			int timeFailed = this.biddingFailRecordMap
					.get(bm.getTask().getId());
			// newUtility=utility*Math.pow(1-this.decayFactor, timeFailed);
			newUtility = utility - (this.decayFactor * timeFailed);
			if (newUtility < 0) {
				return 0;
			}

			// print("#####agent "+this.agentId+" has failed task "+bm.getTask().getId()+" "+timeFailed+
			// "times "+utility+"---> "+newUtility);
			return newUtility;
		}

		else {
			// print("else~~~~");
			return utility;
		}

	}

	/**
	 * this method update agents RejectB
	 */
	public void updateRejectB() {
	}

	public ArrayList<BlackboardMessage> find_K_Neighbor(BlackboardMessage bm) {
		if (agentbidMessageList.size() == 0) {
			return null;
		} else if (agentbidMessageList.size() <= k) {// use whatever it has
			return agentbidMessageList;
		} else {// need to find k Neighbor
			ArrayList<BlackboardMessage> kNeighborList = new ArrayList<BlackboardMessage>();
			for (BlackboardMessage message : agentbidMessageList) {// find
																	// distance
																	// bewteen
																	// bm and
																	// all other
																	// messages
																	// in
																	// agentbidMessageList
				findDistance(bm, message);// find and record the distance in the
											// message, then we sort the message
			}
			// sort the messages in agentbidMessageList according to the
			// distance found
			Collections.sort(agentbidMessageList,
					new BlackboardMessageComparator());

			print("Distance: " + agentbidMessageList.get(0).distance + " , "
					+ agentbidMessageList.get(1).distance + " , "
					+ agentbidMessageList.get(2).distance + " , "
					+ agentbidMessageList.get(3).distance + " , "
					+ agentbidMessageList.get(4).distance);

			// add to the kNeighborList
			for (int i = 0; i < k; i++) {
				kNeighborList.add(agentbidMessageList.get(i));
			}
			return kNeighborList;
		}

	}

	public void findDistance(BlackboardMessage bm1, BlackboardMessage bm2) {
		int size = bm1.getSubtasks().size();

		double SumdiffQual = 0;
		int SumNum = 0;
		for (int i = 0; i < size; i++) {
			double diffQual = bm1.getSubtasks().get(i).getQuality()
					- bm2.getSubtasks().get(i).getQuality();
			SumdiffQual += diffQual * diffQual;
			int diffnum = bm1.getSubtasks().get(i).getNumAgents()
					- bm2.getSubtasks().get(i).getNumAgents();
			SumNum += diffnum * diffnum;
		}

		double distance = Math.sqrt(SumdiffQual + SumNum);
		bm2.distance = distance;
	}

	/**
	 * Return quality of a capability
	 */
	public double getCapabilityQuality(int subtaskId) {// only need to put
														// taskId in here
		return qualityList.get(subtaskId - 1);// agent's cap[0]=taskType1
	}

	/**
	 * 
	 */
	public Integer getId() {
		return this.agentId;
	}

	/**
	 * 
	 * @param capId
	 *            capability ID
	 * @param quality
	 *            quality of the capability
	 */
	public void setCapabilityQuality(int capId, double quality) {
		qualityList.set(capId - 1, quality);
	}

	/**
	 * get busy status of the agent
	 */
	public boolean getBusy() {
		return isBusy;
	}

	/**
	 * set the busy status of the agent
	 */
	public void setBusy(boolean status) {
		isBusy = status;
	}

	public int getNumFailedSubtasks() {
		return numFailedSubtasks;
	}

	public double getAgentOpennessPerception() {
		return agentOpennessPerception;
	}

	public double getTaskOpennessPerception() {
		return taskOpennessPerception;
	}

	/** debug method */
	@SuppressWarnings("unused")
	private void print(String s) {
		if (PrintClass.DebugMode && PrintClass.printClass) {
			System.out.println(this.getClass().getSimpleName() + "::" + s);
		} else if (PrintClass.DebugMode) {
			System.out.println(s);
		}
	}

	public double getIndividualLearnedCap() {
		return individualLearnedCap;
	}

	public void updateIndividualLearnedCap(double LearnedCap) {
		this.individualLearnedCap += LearnedCap;
	}

	public int getTaskAssignmentAtOneTick() {
		return taskAssignmentAtOneTick;
	}

	public int getNumTaskInvolved() {
		return numTaskInvolved;
	}

	public ArrayList<Double> getQualityList() {
		return qualityList;
	}

	public String getQualityListString() {
		String info = new String();
		for (Double quality : this.getQualityList()) {
			info = info + quality + ",";
		}
		return info;
	}

	public int getKnownAgentSetSize() {
		return this.knownAgentSet.size();
	}

	public int getTaskIdForLearningGain() {
		return TaskIdForLearningGain;
	}

	public void setTaskIdForLearningGain(int taskIdForLearningGain) {
		TaskIdForLearningGain = taskIdForLearningGain;
	}

	public double getTaskLearningGain() {
		return TaskLearningGain;
	}

	public void setTaskLearningGain(double taskLearningGain) {
		TaskLearningGain = taskLearningGain;
	}

	public boolean isBusy() {
		return isBusy;
	}

	public int getTaskTobidAtOneTick() {
		return TaskTobidAtOneTick;
	}

	public void setTaskTobidAtOneTick(int taskTobidAtOneTick) {
		TaskTobidAtOneTick = taskTobidAtOneTick;
	}

	public int getNumberOfAvailableTasksAtOneTick() {
		return NumberOfAvailableTasksAtOneTick;
	}

	public void setNumberOfAvailableTasksAtOneTick(
			int numberOfAvailableTasksAtOneTick) {
		NumberOfAvailableTasksAtOneTick = numberOfAvailableTasksAtOneTick;
	}

	public String getTaskId_And_LearningGain() {
		String s = this.TaskIdForLearningGain + "," + this.TaskLearningGain;
		return s;
	}

	public int getAgentType() {
		return agentType;
	}

	public void setAgentType(int agentType) {
		this.agentType = agentType;
	}

	public double getF_RA() {
		return F_RA;
	}

	public void setF_RA(double f_RA) {
		F_RA = f_RA;
	}

	public int getRejectA() {
		return RejectA;
	}

	public void setRejectA(int rejectA) {
		RejectA = rejectA;
	}

	public int[] getRejectAList() {
		return RejectAList;
	}

	public void setRejectAList(int[] rejectAList) {
		RejectAList = rejectAList;
	}

	public double getF() {
		return f;
	}

	public void setF(double f) {
		this.f = f;
	}

	public int[] getQualThreshAboveList() {
		return qualThreshAboveList;
	}

	public void setQualThreshAboveList(int[] qualThreshAboveList) {
		this.qualThreshAboveList = qualThreshAboveList;
	}

	public int[] getNumSubtaskEvaluatedList() {
		return numSubtaskEvaluatedList;
	}

	public void setNumSubtaskEvaluatedList(int[] numSubtaskEvaluatedList) {
		this.numSubtaskEvaluatedList = numSubtaskEvaluatedList;
	}

	public double getG() {
		return g;
	}

	public void setG(double g) {
		this.g = g;
	}

	public int[] getNumAgentsAssignedList() {
		return numAgentsAssignedList;
	}

	public void setNumAgentsAssignedList(int[] numAgentsAssignedList) {
		this.numAgentsAssignedList = numAgentsAssignedList;
	}

	public int[] getNumAgentsRequiredList() {
		return numAgentsRequiredList;
	}

	public void setNumAgentsRequiredList(int[] numAgentsRequiredList) {
		this.numAgentsRequiredList = numAgentsRequiredList;
	}

	public double getF_RB() {
		return F_RB;
	}

	public void setF_RB(double f_RB) {
		F_RB = f_RB;
	}

	public int getRejectB() {
		return RejectB;
	}

	public void setRejectB(int rejectB) {
		RejectB = rejectB;
	}

	public int[] getRejectBList() {
		return RejectBList;
	}

	public void setRejectBList(int[] rejectBList) {
		RejectBList = rejectBList;
	}

	public int getBidsSubmitted() {
		return bidsSubmitted;
	}

	public void setBidsSubmitted(int bidsSubmitted) {
		this.bidsSubmitted = bidsSubmitted;
	}

	public int[] getBidsSumbittedList() {
		return bidsSumbittedList;
	}

	public void setBidsSumbittedList(int[] bidsSumbittedList) {
		this.bidsSumbittedList = bidsSumbittedList;
	}

	public int getQualThreshAbove() {
		return qualThreshAbove;
	}

	public void setQualThreshAbove(int qualThreshAbove) {
		this.qualThreshAbove = qualThreshAbove;
	}

	public int getNumSubtaskEvaluated() {
		return numSubtaskEvaluated;
	}

	public void setNumSubtaskEvaluated(int numSubtaskEvaluated) {
		this.numSubtaskEvaluated = numSubtaskEvaluated;
	}

	public int getNumAgentsAssigned() {
		return numAgentsAssigned;
	}

	public void setNumAgentsAssigned(int numAgentsAssigned) {
		this.numAgentsAssigned = numAgentsAssigned;
	}

	public int getNumAgentsRequired() {
		return numAgentsRequired;
	}

	public void setNumAgentsRequired(int numAgentsRequired) {
		this.numAgentsRequired = numAgentsRequired;
	}

	public int getBidsSubmittedAndWon() {
		return bidsSubmittedAndWon;
	}

	public void setBidsSubmittedAndWon(int bidsSubmittedAndWon) {
		this.bidsSubmittedAndWon = bidsSubmittedAndWon;
	}

	public int[] getBidsSubmittedAndWonList() {
		return bidsSubmittedAndWonList;
	}

	public void setBidsSubmittedAndWonList(int[] bidsSubmittedAndWonList) {
		this.bidsSubmittedAndWonList = bidsSubmittedAndWonList;
	}

	public double getP_wb() {
		return P_wb;
	}

	public void setP_wb(double p_wb) {
		P_wb = p_wb;
	}

	public double getP_off_wb() {
		return P_off_wb;
	}

	public void setP_off_wb(double p_off_wb) {
		P_off_wb = p_off_wb;
	}

	public int getRejectARollingSum() {
		return RejectARollingSum;
	}

	public void setRejectARollingSum(int rejectARollingSum) {
		RejectARollingSum = rejectARollingSum;
	}

	public int getBidsSubmittedRollingSum() {
		return bidsSubmittedRollingSum;
	}

	public void setBidsSubmittedRollingSum(int bidsSubmittedRollingSum) {
		this.bidsSubmittedRollingSum = bidsSubmittedRollingSum;
	}

	public int getRejectBRollingSum() {
		return RejectBRollingSum;
	}

	public void setRejectBRollingSum(int rejectBRollingSum) {
		RejectBRollingSum = rejectBRollingSum;
	}

	public int getBidSubmittedAndWonRollingSum() {
		return bidSubmittedAndWonRollingSum;
	}

	public void setBidSubmittedAndWonRollingSum(int bidSubmittedAndWonRollingSum) {
		this.bidSubmittedAndWonRollingSum = bidSubmittedAndWonRollingSum;
	}

	public double getReward() {
		return reward;
	}

	public void setReward(double reward) {
		this.reward = reward;
	}

	public boolean isSelectedForCurrentBid() {
		return selectedForCurrentBid;
	}

	public void setSelectedForCurrentBid(boolean selectedForCurrentBid) {
		this.selectedForCurrentBid = selectedForCurrentBid;
	}

	public int getNumBidsSubmitted() {
		return numBidsSubmitted;
	}

	public void setNumBidsSubmitted(int numBidsSubmitted) {
		this.numBidsSubmitted = numBidsSubmitted;
	}

	public int getNumbidsSubmittedAndWon() {
		return numbidsSubmittedAndWon;
	}

	public void setNumbidsSubmittedAndWon(int numbidsSubmittedAndWon) {
		this.numbidsSubmittedAndWon = numbidsSubmittedAndWon;
	}

	public boolean isNeverAssignedJob() {
		return neverAssignedJob;
	}

	public void setNeverAssignedJob(boolean neverAssignedJob) {
		this.neverAssignedJob = neverAssignedJob;
	}

	public int getTaskTypebidAtOneTick() {
		return TaskTypebidAtOneTick;
	}

	public void setTaskTypebidAtOneTick(int taskTypebidAtOneTick) {
		TaskTypebidAtOneTick = taskTypebidAtOneTick;
	}

	public double getAgentOpenness() {
		return this.bb.getAgentOpenness();
	}

	public double getTaskOpenness() {
		return this.bb.getTaskOpenness();
	}

	public int getOption() {
		return this.bb.getOption();
	}

	public int getNumSubtasksAssignedAtOneTick() {
		return numSubtasksAssignedAtOneTick;
	}

	public void setNumSubtasksAssignedAtOneTick(int numSubtasksAssignedAtOneTick) {
		this.numSubtasksAssignedAtOneTick = numSubtasksAssignedAtOneTick;
	}

	public double getRewardAtCurrentTick() {
		return rewardAtCurrentTick;
	}

	public void setRewardAtCurrentTick(double rewardAtCurrentTick) {
		this.rewardAtCurrentTick = rewardAtCurrentTick;
	}

	public double getTaskReward() {
		return taskReward;
	}

	public void setTaskReward(double taskReward) {
		this.taskReward = taskReward;
	}

	public double getSelfGainAtOneTick() {
		return selfGainAtOneTick;
	}

	public void setSelfGainAtOneTick(double selfGainAtOneTick) {
		this.selfGainAtOneTick = selfGainAtOneTick;
	}

	public double getObservationGainAtOneTick() {
		return observationGainAtOneTick;
	}

	public void setObservationGainAtOneTick(double observationGainAtOneTick) {
		this.observationGainAtOneTick = observationGainAtOneTick;
	}

	public String getAgentOutputWithCap() {
		String output = String
				.format("%d,%d,%d,%d,%.4f,%d,%d,%d,%s,%s,%d,%d,%.2f,%.2f,%d,%d,%d,%.4f,%d,%.4f,%.4f,%.4f,%d,%d,%s",
						MainAgent.tick, this.getId(),
						this.getNumTaskInvolved(),
						this.getTaskAssignmentAtOneTick(), this.getReward(),
						this.TaskTobidAtOneTick, this.getNumBidsSubmitted(),
						this.getNumbidsSubmittedAndWon(),
						this.selectedForCurrentBid, "1",
						this.TaskTypebidAtOneTick, MainAgent.randomSeed,
						this.getAgentOpenness(), this.getTaskOpenness(),
						this.option, this.getNumAgentsAssigned(),
						this.getNumAgentsRequired(),
						this.getRewardAtCurrentTick(),
						this.getNumSubtasksAssignedAtOneTick(),
						this.getTaskReward(), this.getSelfGainAtOneTick(),
						this.getObservationGainAtOneTick(),
						this.ObservedCapUsedCount,
						this.ObservedCapUsedCountAtOneTick, this.getAgentCap());
		return output;
	}

	public String getAgentOutput() {
		String output = String
				.format("%d,%d,%d,%d,%.4f,%d,%d,%d,%s,%s,%d,%d,%.2f,%.2f,%d,%d,%d,%.4f,%d,%.4f,%.4f,%.4f,%d,%d,%d",
						MainAgent.tick, this.getId(),
						this.getNumTaskInvolved(),
						this.getTaskAssignmentAtOneTick(), this.getReward(),
						this.TaskTobidAtOneTick, this.getNumBidsSubmitted(),
						this.getNumbidsSubmittedAndWon(),
						this.selectedForCurrentBid, "1",
						this.TaskTypebidAtOneTick, MainAgent.randomSeed,
						this.getAgentOpenness(), this.getTaskOpenness(),
						this.option, this.getNumAgentsAssigned(),
						this.getNumAgentsRequired(),
						this.getRewardAtCurrentTick(),
						this.getNumSubtasksAssignedAtOneTick(),
						this.getTaskReward(), this.getSelfGainAtOneTick(),
						this.getObservationGainAtOneTick(),
						this.ObservedCapUsedCount,
						this.ObservedCapUsedCountAtOneTick, this.optionType);
		return output;
	}

	public String getAgentCapOutput() {
		StringBuilder sb = new StringBuilder();
		for (double d : this.qualityList) {
			sb.append(d + " ");
		}
		String output = String.format("%d,%d,%s,", this.getId(),
				MainAgent.tick, sb.toString());
		return output;
	}

	public String getAgentCap() {
		StringBuilder sb = new StringBuilder();
		for (double d : this.qualityList) {
			sb.append(d + " ");
		}
		// String
		// output=String.format("%d,%d,%s,",this.getId(),MainAgent.tick,sb.toString());
		return sb.toString();
	}

	public int getOptionType() {
		return optionType;
	}

	public void setOptionType(int optionType) {
		this.optionType = optionType;
	}

}// end class

/**
 * 
 * @author Xi Chen Compare two instance of Agent base on a specify capability
 *         Return -1 if agent a has higher quality of that capability Return 0
 *         if agent a has same quality of that capability return 1 if agent a
 *         has lower quality of that capability
 */
class AgentQualityComparator implements Comparator<Agent> {
	int capId;

	AgentQualityComparator(int id) {
		capId = id;
	}

	@Override
	public int compare(Agent a, Agent b) {
		return a.getCapabilityQuality(capId) < b.getCapabilityQuality(capId) ? 1
				: a.getCapabilityQuality(capId) == b
						.getCapabilityQuality(capId) ? 0 : -1;
	}

}

/**
 * 
 * @author BinChen Compare two blackboardMessage to sort them according to the
 *         distance of the task calculated before
 */
class BlackboardMessageComparator implements Comparator<BlackboardMessage> {
	BlackboardMessageComparator() {
	}

	@Override
	public int compare(BlackboardMessage a, BlackboardMessage b) {
		return a.distance > b.distance ? 1 : a.distance == b.distance ? 0 : -1;
	}
}

/***************************************************/

/**
 * @author Anish Helper class for AgentSpecification Just gets quality between
 *         Minimum and Maximum Quality Value Note to Self: Collapse this class
 *         to a method inside AgentQualityHelper Class.
 */
class Capabilities {

	private double Quality;

	public double getQuality() {
		return Quality;
	}

	public void setQuality(double quality) {
		Quality = quality;
	}

	public void MakeAgentWithQualityBetween(double MinQuality, double MaxQuality) {

		this.Quality = round1d(MinQuality + Math.random()
				* (MaxQuality - MinQuality));

	}

	public static double round1d(double a) {
		return a;
	}// end of method
}

/**
 * AgentQualityHelper - Helper class to generate Capabilities value for any
 * agent type
 * 
 * @return : double[] AgentSpec with capability values that corresponds to
 *         expert, average or novice agents For example, it can return
 *         [0.9,0.9,0.3,0.3,0.3] - Which is the capability of an expert agent
 *         etc.
 */
class AgentQualityHelper {

	private double[] AgentSpec;

	public double[] GetExpertAgent(int NumOfCapabilities) {

		int NumOfExpertCapabilities = (int) (Math.ceil(NumOfCapabilities / 3.0));
		AgentSpec = new double[NumOfCapabilities];
		int i = 0;

		while (i < NumOfExpertCapabilities) {
			Capabilities Temp = new Capabilities();
			Temp.MakeAgentWithQualityBetween(0.7, 1.0);
			AgentSpec[i] = round1d(Temp.getQuality());
			i++;
		}

		while (i < NumOfCapabilities) {
			Capabilities Temp = new Capabilities();
			AgentSpec[i] = round1d(Math.random() * (1.0));
			i++;
		}

		return AgentSpec;
	}

	public double[] GetAverageAgent(int NumOfCapabilities) {

		int NumOfAverageCapabilities = (int) (Math
				.ceil(NumOfCapabilities / 3.0)); // Make the task Average
													// SubTask Dominant ;

		int NumOfExpertCapabilities = (int) (Math.random() * Math
				.floor(NumOfCapabilities / 3));
		;

		AgentSpec = new double[NumOfCapabilities];
		int i = 0;

		while (i < NumOfExpertCapabilities) {
			Capabilities Temp = new Capabilities();
			Temp.MakeAgentWithQualityBetween(0.7, 1.0);
			AgentSpec[i] = round1d(Temp.getQuality());
			i++;
		}

		while (i < NumOfAverageCapabilities + NumOfExpertCapabilities) {
			Capabilities Temp = new Capabilities();
			Temp.MakeAgentWithQualityBetween(0.3, 0.699999);
			AgentSpec[i] = round1d(Temp.getQuality());
			i++;
		}

		while (i < NumOfCapabilities) {
			Capabilities Temp = new Capabilities();
			Temp.MakeAgentWithQualityBetween(0.0, 0.699999);
			AgentSpec[i] = round1d(Temp.getQuality());
			i++;
		}

		return AgentSpec;
	}

	public double[] GetNoviceAgent(int NumOfCapabilities) {

		int NumOfExpertCapabilities = (int) (Math.random() * Math
				.floor(NumOfCapabilities / 3));
		int NumOfAverageCapabilities = (int) (Math.random() * Math
				.floor(NumOfCapabilities / 3));

		int NumOfNoviceCapabilities = NumOfCapabilities
				- (NumOfExpertCapabilities + NumOfAverageCapabilities);

		AgentSpec = new double[NumOfCapabilities];
		int i = 0;

		while (i < NumOfNoviceCapabilities) {
			Capabilities Temp = new Capabilities();
			Temp.MakeAgentWithQualityBetween(0.0, 0.299999);
			AgentSpec[i] = round1d(Temp.getQuality());
			i++;
		}

		while (i < NumOfNoviceCapabilities + NumOfExpertCapabilities) {
			Capabilities Temp = new Capabilities();
			Temp.MakeAgentWithQualityBetween(0.7, 1.0);
			AgentSpec[i] = round1d(Temp.getQuality());
			i++;
		}

		while (i < NumOfCapabilities) {
			Capabilities Temp = new Capabilities();
			Temp.MakeAgentWithQualityBetween(0.3, 0.699999);
			AgentSpec[i] = round1d(0.1 + Math.random() * (1.0 - 0.1));
			i++;
		}

		return AgentSpec;
	}

	public static double round1d(double a) {
		return a;
	}// end of method

	public String CheckType(double[] AgentSpec) {

		int ExpertCount = 0, NoviceCount = 0, AverageCount = 0;

		for (int i = 0; i < AgentSpec.length; i++) {
			if (AgentSpec[i] >= 0.7) {
				ExpertCount++;
			} else if (AgentSpec[i] >= 0.3) {
				AverageCount++;
			} else {
				NoviceCount++;
			}
		}

		if ((double) ExpertCount >= AgentSpec.length / 3) {
			// print("Expert");

			return "Expert";
		}

		else if ((double) AverageCount >= AgentSpec.length / 3) {
			// print("Average");
			return "Average";
		}

		else {
			// print("Novice");
			return "Novice";
		}
	}

}// end class

