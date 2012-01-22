package org.gradle.api.internal.artifacts.dsl;

import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.GroovyCodeVisitor;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import org.codehaus.groovy.classgen.BytecodeExpression;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.gradle.api.specs.Spec;

/**
 * Created by IntelliJ IDEA. User: merlyna Date: 1/20/12 Time: 9:11 PM To change this template use File | Settings | File Templates.
 */
public class ApplyPluginScriptTransformer extends AbstractScriptTransformer {

    public static class IsApplyPluginStatement implements Spec<Statement> {
        public boolean isSatisfiedBy(Statement element) {
            final MapEntryExpression mapEntryExpression = findMapEntryExpression(element);
            if (mapEntryExpression != null) {
                return true;
            } else {
                return false;
            }
        }

        public MapEntryExpression findMapEntryExpression(Statement statement) {
            if (!(statement instanceof ExpressionStatement)) {
                return null;
            }

            ExpressionStatement expressionStatement = (ExpressionStatement) statement;
            if (!(expressionStatement.getExpression() instanceof MethodCallExpression)) {
                return null;
            }

            MethodCallExpression methodCall = (MethodCallExpression) expressionStatement.getExpression();
            if (!isMethodOnThis(methodCall, "apply")) {
                return null;
            }

            if (!(methodCall.getArguments() instanceof TupleExpression)) {
                return null;
            }

            TupleExpression tuple = (TupleExpression) methodCall.getArguments();
            final Expression expression = tuple.getExpression(0);
            if (!(expression instanceof NamedArgumentListExpression)) {
                return null;
            }

            NamedArgumentListExpression namedArgumentListExpression = (NamedArgumentListExpression) expression;
            final MapEntryExpression mapEntryExpression = namedArgumentListExpression.getMapEntryExpressions().get(0);
            final Expression keyExpression = mapEntryExpression.getKeyExpression();
            if (!(keyExpression instanceof ConstantExpression)) {
                return null;
            }

            ConstantExpression keyConstantExpression = (ConstantExpression) keyExpression;
            if (!"plugin".equals(keyConstantExpression.getText())) {
                return null;
            }

            final Expression valueExpression = mapEntryExpression.getValueExpression();
            if (!(valueExpression instanceof ConstantExpression)) {
                return null;
            }

            ConstantExpression valueConstantExpression = (ConstantExpression) valueExpression;
            if (valueConstantExpression.getText() == null || !valueConstantExpression.getText().contains(":")) {
                return null;
            }
            
            return mapEntryExpression;
        }
    }

    @Override
    protected int getPhase() {
        return Phases.CONVERSION;
    }

    @Override
    public void call(SourceUnit source) throws CompilationFailedException {
        visitScriptCode(source, new ApplyPluginDefinitionTransformer());
    }

    public String getId() {
        return "applyPlugin";
    }

    private class ApplyPluginDefinitionTransformer extends CodeVisitorSupport {
        IsApplyPluginStatement spec = new IsApplyPluginStatement();

        @Override
        public void visitExpressionStatement(ExpressionStatement statement) {
            final MapEntryExpression mapEntryExpression = spec.findMapEntryExpression(statement);
            if (mapEntryExpression != null) {
                final String value = mapEntryExpression.getValueExpression().getText();
                mapEntryExpression.setValueExpression(new ConstantExpression(value.substring(value.lastIndexOf(':')+1)));
            }
        }
    }
}
