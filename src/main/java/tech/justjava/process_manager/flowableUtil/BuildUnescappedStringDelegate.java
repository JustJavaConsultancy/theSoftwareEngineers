package tech.justjava.process_manager.flowableUtil;

import org.apache.commons.lang3.StringEscapeUtils;
import org.flowable.common.engine.impl.el.FixedValue;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component
public class BuildUnescappedStringDelegate implements JavaDelegate {

    private FixedValue variableToEscape;

    public void setVariableToEscape(FixedValue variableToEscape) {
        this.variableToEscape = variableToEscape;
    }


    @Override
    public void execute(DelegateExecution execution) {
        String value = (String) execution.getVariable(variableToEscape.getExpressionText());
        System.out.println(" The Value retrieved ==="+value);
        String unescapedValue = StringEscapeUtils.unescapeJava(value);
        System.out.println(" The Value unescaped ==="+unescapedValue);
        execution.setVariable(variableToEscape.getExpressionText()+"_unescaped", unescapedValue);
    }
}
