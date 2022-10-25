package tinygp;

import org.matheclipse.core.eval.ExprEvaluator;
import org.matheclipse.core.interfaces.IExpr;

public class Optimizer {
    public String optimize(String function) {
        ExprEvaluator evaluator = new ExprEvaluator();
        IExpr result = evaluator.eval(function);

        return result.toString();
    }
}