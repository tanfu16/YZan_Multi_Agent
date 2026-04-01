package com.yzan.yzan_multi_agent.workflow;


import com.yzan.yzan_multi_agent.agent.*;
import com.yzan.yzan_multi_agent.domain.AgentResult;
import com.yzan.yzan_multi_agent.domain.DecorationPlan;
import com.yzan.yzan_multi_agent.domain.StructuredRequirement;
import com.yzan.yzan_multi_agent.domain.UserRequirement;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DecorationWorkflowService {

    private final RequirementAgent requirementAgent = new RequirementAgent();
    private final DecorationAgent layoutAgent = new LayoutAgent();
    private final DecorationAgent budgetAgent = new BudgetAgent();
    private final DecorationAgent safetyAgent = new SafetyAgent();
    private final CoordinatorAgent coordinatorAgent = new CoordinatorAgent();

    public DecorationPlan execute(UserRequirement userRequirement){
        StructuredRequirement structuredRequirement = requirementAgent.execute(userRequirement);

        List<AgentResult> results = new ArrayList<>();
        results.add(layoutAgent.execute(structuredRequirement));
        results.add(budgetAgent.execute(structuredRequirement));
        results.add(safetyAgent.execute(structuredRequirement));

        return coordinatorAgent.execute(structuredRequirement, results);
    }
}
