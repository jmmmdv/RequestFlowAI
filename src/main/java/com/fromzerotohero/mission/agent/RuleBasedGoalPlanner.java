package com.fromzerotohero.mission.agent;

import org.springframework.stereotype.Component;

@Component
public class RuleBasedGoalPlanner implements GoalPlanner {
    @Override
    public String classify(String goal) {
        if (goal.matches(".*(test|quality|bug|playwright).*")) return "QUALITY_ENGINEERING";
        if (goal.matches(".*(deploy|aws|cloud|jenkins|release).*")) return "DELIVERY_AUTOMATION";
        if (goal.matches(".*(api|spring|backend|rest).*")) return "SOFTWARE_DEVELOPMENT";
        return "INTELLIGENT_AUTOMATION";
    }
}
