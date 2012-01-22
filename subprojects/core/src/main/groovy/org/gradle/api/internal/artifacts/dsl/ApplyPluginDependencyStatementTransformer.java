package org.gradle.api.internal.artifacts.dsl;

import org.codehaus.groovy.ast.CodeVisitorSupport;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.VariableScope;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ExpressionStatement;
import org.codehaus.groovy.ast.stmt.Statement;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.control.Phases;
import org.codehaus.groovy.control.SourceUnit;
import org.gradle.api.specs.Spec;

import java.util.List;

/**
 * Created by IntelliJ IDEA. User: merlyna Date: 1/22/12 Time: 2:03 PM To change this template use File | Settings | File Templates.
 */
public class ApplyPluginDependencyStatementTransformer extends AbstractScriptTransformer {

    @Override
    protected int getPhase() {
        return Phases.CONVERSION;
    }

    public String getId() {
        return "applyPluginDependency";
    }

    @Override
    public void call(SourceUnit source) throws CompilationFailedException {
        visitScriptCode(source, new CodeVisitorSupport() {
            @Override
            public void visitExpressionStatement(ExpressionStatement statement) {
                final NamedArgumentListExpression argumentList = findArgumentList(statement);
                if (argumentList != null) {
                    transformStatement(statement, argumentList);
                }
            }
        });
    }

    public void transformStatement(ExpressionStatement statement, NamedArgumentListExpression argumentList) {
        String dependency = getMapValue(argumentList, "dependency");

        // Expression for:
        //   buildscript { classpathDependency( dependency ) }

        MethodCallExpression buildscriptCall = new MethodCallExpression(
                new VariableExpression("this"),
                new ConstantExpression("buildscript"),
                new ArgumentListExpression(
                        new ClosureExpression(new Parameter[0], new BlockStatement(new Statement[] {
                                new ExpressionStatement(
                                        new MethodCallExpression(
                                                new VariableExpression("this"),
                                                new ConstantExpression("classpathDependency"),
                                                new ArgumentListExpression(new ConstantExpression(dependency))
                                        )
                                )
                        }, new VariableScope()))
                )
        );

        statement.setExpression(buildscriptCall);
    }

    private String getMapValue(NamedArgumentListExpression argumentList, String key) {
        String value = null;

        final List<MapEntryExpression> mapEntryExpressions = argumentList.getMapEntryExpressions();
        for(MapEntryExpression entry : mapEntryExpressions) {
            if (key.equals(entry.getKeyExpression().getText())) {
                value = entry.getValueExpression().getText();
                break;
            }
        }

        return value;
    }

    public Spec<Statement> getSpec() {
        return new Spec<Statement>() {
            public boolean isSatisfiedBy(Statement element) {
                return findArgumentList(element) != null;
            }
        };
    }

    private NamedArgumentListExpression findArgumentList(Statement statement) {
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
        String dependency = getMapValue(namedArgumentListExpression, "dependency");
        if (dependency == null) {
            return null;
        }

        return namedArgumentListExpression;
    }

}
