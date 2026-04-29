package com.yzan.yzan_multi_agent.skills;

public interface SkillInvocationService {

    SkillExecutionResult execute(String userRequest,
                                 String location,
                                 String materialKeyword,
                                 String platform,
                                 String furnitureKeyword);

    SkillExecutionResult executeMaterialStoreSkill(String userRequest,
                                                   String location,
                                                   String materialKeyword);

    SkillExecutionResult executeFurnitureSearchSkill(String userRequest,
                                                     String platform,
                                                     String furnitureKeyword);
}
