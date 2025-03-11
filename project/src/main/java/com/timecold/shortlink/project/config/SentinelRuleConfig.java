package com.timecold.shortlink.project.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SentinelRuleConfig implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        // 创建短链接的限流规则
        List<FlowRule> rules = new ArrayList<>();
        FlowRule createOrderRule = new FlowRule();
        createOrderRule.setResource("create_short-link");
        createOrderRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        createOrderRule.setCount(10);
        rules.add(createOrderRule);

        // 分页查询的限流规则
        FlowRule pageShortLinkRule = new FlowRule();
        pageShortLinkRule.setResource("page_short-link");
        pageShortLinkRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        pageShortLinkRule.setCount(10);
        rules.add(pageShortLinkRule);

        FlowRuleManager.loadRules(rules);
    }
}
