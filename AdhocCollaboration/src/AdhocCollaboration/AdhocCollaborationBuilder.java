package AdhocCollaboration;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Random;

import cern.jet.random.Uniform;
import repast.simphony.context.Context;
import repast.simphony.context.DefaultContext;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameters;
import repast.simphony.util.ContextUtils;
import repast.simphony.util.collections.IndexedIterable;


public class AdhocCollaborationBuilder extends DefaultContext<Object> implements ContextBuilder<Object> {

	
	/* (non-Javadoc)
	 * @see repast.simphony.dataLoader.ContextBuilder#build(repast.simphony.context.Context)
	 */
	@Override
	public Context<Object> build(Context<Object> context) {
		//context.clear();
		context.setId("AdhocCollaboration");
		//read in parameters 
		Parameters params 		= RunEnvironment.getInstance().getParameters();
		int initalCapNummber		=(Integer) params.getValue ("initalCapNum");/*Initial Number of non zero capabilities*/
		int agentCount 			= (Integer) params.getValue ("agent_count");
		double taskOpenness 	= (Double) params.getValue("task_openness");
		int totalTick           =(Integer) params.getValue ("total_tick");
		double agentOpenness 	= (Double) params.getValue("agent_openness"); 
		int option 			= (Integer) params.getValue ("option");	//agents has different task selection strategies, when option=99, agents can have different task selection stretegies in 
		// one simulation according to its "optionType".
		
		
		/**
		 * this determines the percentage of agents using each type of task selection strategies, 
		 * option14-option15-option16-option17
		 * 30-20-30-20 means 30% of agents use option14, 20% use option15,30% use option16 and 20%use option17
		 * 50-0-0-50 means 50% of agents use option14, 50% use option17
		 */
		String optionTypeDistrubution=(String) params.getValue("optionTypeDistrubution");
		
		
		
		
		
		
		
		
		/**
		 * Disable agentTypes function by entering an 0, this way agent will be randomly generated, we don't care about agent types. After kill, we also randomly entroduce an agent 
		 */
		int UsingAgentDistrubution= (Integer) params.getValue ("UsingAgentDistrubution");
		/**
		 * Disable task Types function by entering an 0, this way all tasks are randomly generated, they are no task types, we use"config_typesRandom.properties" to draw all the tasks from
		 */
		int UsingTaskDistrubution=(Integer) params.getValue ("UsingTaskDistrubution");
		
		
		int randomSeed = (Integer) params.getValue("randomSeed");
	
		
		int agentType 			= 0; /* 1 - expert, 2 - average ,3-novice*/
		double agentQualityMax 	= 1;
		int ticksToFinish = (Integer) params.getValue ("tickToFinish");/*ticks required for finishing a task, right now every task has the needs same tick to finish.*/
		
		/*user enter a string of two numbers, not separated by comma. Example: 3040  .this means 30% of Expert, 40% of Average, and 100-(30+40) % of Novice*/
		String AgentDistrubution =  (String) params.getValue("AgentDistrubution");
		
		/*user enter a string of two numbers, not separated by comma. Example: 3333  .this means 33% of HardTask, 33% of AverageTask, and 100-(33+33) % of EasyTask*/
		String TaskDistrubution =  (String) params.getValue("TaskDistrubution");
		
		String HardPercentageStr, AveragePercentageStr;
		HardPercentageStr="HP"+TaskDistrubution.substring(0,2);
		AveragePercentageStr="AP"+TaskDistrubution.substring(2,4);
		
		
		
		
		
		/*
		 * creating a new blackboard
		 */
		Blackboard b 			= new Blackboard();	
		b.setOption(option);
		b.setOptionTypeDistrubution(optionTypeDistrubution);
		b.setAgentCount(agentCount);
		b.setNumUnassignedAgents(agentCount);
		b.setAgentOpenness(agentOpenness);
		b.setTaskOpenness(taskOpenness);
		b.setUsingAgentDistrubution(UsingAgentDistrubution);
		b.setUsingTaskDistrubution(UsingTaskDistrubution);
		b.setNumAgentsIntroduced(agentCount);
		context.add(b);
		
		print("Task openness "+taskOpenness);
		// pass parameters to the main agent
		MainAgent mainAgent = null;
		try {
			mainAgent = new MainAgent(b,taskOpenness, totalTick,agentCount, agentType, agentOpenness,initalCapNummber,HardPercentageStr,AveragePercentageStr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mainAgent.setTaskDistrubution(TaskDistrubution);//pass taskDistribution to the mainAgent, later the mainAgent will choose the congfig file based on the distrubution to pick task orders
		mainAgent.setTicksToFinish(ticksToFinish);//pass tickTOFinish to mainAgent, so it can allow the last auctioned off task to have extra tick to get finished.
		mainAgent.setContext(context);
		mainAgent.setUsingAgentDistrubution(UsingAgentDistrubution);
		mainAgent.setUsingTaskDistrubution(UsingTaskDistrubution);
//		Random random = new Random( randomSeed);
		Uniform uniform= new Uniform(0.0,1.0, randomSeed); //the RNG for the environment 
		mainAgent.setUniform(uniform);
		context.add(mainAgent);
		MainAgent.randomSeed= randomSeed;
		
		b.mainAgent=mainAgent;
		
		/*
		 * create agents
		 */
		if (UsingAgentDistrubution==1){//using agent Types and create agents according to the agent distribution
			/*
			 * start the agents, create agent types depending on agentTypeScenario
			 */
			int expertCount=0;int averageCount=0; int noviceCount=0;
			//String[] token=distrubution.split(",");
			
			String expertCountStr, averageCountStr;
			expertCountStr = AgentDistrubution.substring(0,2);
			averageCountStr = AgentDistrubution.substring(2,4);
			
			expertCount=(int) Math.round((Integer.parseInt(expertCountStr)*agentCount/100.0)) ;
			averageCount=(int) Math.round((Integer.parseInt(averageCountStr)*agentCount/100.0));
			noviceCount=agentCount-expertCount-averageCount;
			
			for(int i=0; i<agentCount; i++) {
				if(i < expertCount){
					agentType= 1; // Expert
				}
				else if (i <expertCount + averageCount){
	;				agentType = 2; // Average
				}
				else{
					agentType =3; // Novice
				}
		
				Agent newAgent=new Agent(i,b,agentType,agentQualityMax,initalCapNummber);
				context.add(newAgent);
				/* adding this agent to bb's agentList*/
				b.addAgent(newAgent);
				/*adding this agent to bb's AgentMap*/
				b.AddToAgentMap(i, newAgent);	
			}

		}else{//do not use agent type, we randomly generate agents 
			
		
			
			
			if (option==99){
				int op14count=0;
				int op15count=0;
				int op16count=0;
				int op17count=0;
				HashMap<String,Integer> optionCountMap = new HashMap<String,Integer>();
				
//				optionTypeDistrubution=(String) params.getValue("optionTypeDistrubution");
				
				if (optionTypeDistrubution!=null && optionTypeDistrubution!=""){
					String token[]=optionTypeDistrubution.split("-");
			
					if (Double.parseDouble(token[0])!=0){
						 op14count=(int) ((Double.parseDouble(token[0])/100)*agentCount);
						
					}else{
						op14count=0;
					}
					 optionCountMap.put("14", op14count);
					 
					if (Double.parseDouble(token[1])!=0){
						op15count=(int) ((Double.parseDouble(token[1])/100)*agentCount);
					}else{
						op15count=0;
					}
					 optionCountMap.put("15", op15count);
					 
					if (Double.parseDouble(token[2])!=0){
						op16count=(int) ((Double.parseDouble(token[2])/100)*agentCount);
					}else{
						op16count=0;
					}
					 optionCountMap.put("16", op16count);
					 
					if (Double.parseDouble(token[3])!=0){
						op17count=(int) ((Double.parseDouble(token[3])/100)*agentCount);
					}else{
						op17count=0;
					}
					 optionCountMap.put("17", op17count);
					
				}
				
				print("op14Count:"+op14count);
				print("op15Count:"+op15count);
				print("op16Count:"+op16count);
				print("op17Count:"+op17count);
				print(optionTypeDistrubution);
				print(optionCountMap.toString());
				int OpCount;
				int id=0;
				for (String op : optionCountMap.keySet()){
					OpCount=optionCountMap.get(op);
					for(int i=0; i<OpCount; i++) {
						id++;
						Agent newAgent=new Agent(id,b,agentType,agentQualityMax,initalCapNummber);
						newAgent.setOptionType(Integer.parseInt(op));
						context.add(newAgent);
						/* adding this agent to bb's agentList*/
						b.addAgent(newAgent);
						/*adding this agent to bb's AgentMap*/
						b.AddToAgentMap(i, newAgent);	
						
						newAgent.outputAgentCapToFile();
					}
				
				}
				
				
				
				
				
			}else{
				
				for(int i=0; i<agentCount; i++) {
					
					Agent newAgent=new Agent(i,b,agentType,agentQualityMax,initalCapNummber);
					context.add(newAgent);
					/* adding this agent to bb's agentList*/
					b.addAgent(newAgent);
					/*adding this agent to bb's AgentMap*/
					b.AddToAgentMap(i, newAgent);	
					
					newAgent.outputAgentCapToFile();
				}
				
				
			}
			
			
			
		}
		
		
		
		
		
		
		
		

		print("return context");
		return context;
	}
	
	
	
	public int getAgentCount () {
		return getObjects (Agent.class).size ();
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
    
    
    
   
    
    
}



