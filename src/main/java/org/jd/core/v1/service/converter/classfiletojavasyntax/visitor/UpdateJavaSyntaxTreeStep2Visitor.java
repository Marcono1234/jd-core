/*
 * Copyright (c) 2008, 2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.core.v1.service.converter.classfiletojavasyntax.visitor;

import org.jd.core.v1.model.javasyntax.AbstractJavaSyntaxVisitor;
import org.jd.core.v1.model.javasyntax.declaration.*;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileBodyDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.model.javasyntax.declaration.ClassFileEnumDeclaration;
import org.jd.core.v1.service.converter.classfiletojavasyntax.util.TypeMaker;

public class UpdateJavaSyntaxTreeStep2Visitor extends AbstractJavaSyntaxVisitor {
    protected static final AggregateFieldsVisitor AGGREGATE_FIELDS_VISITOR = new AggregateFieldsVisitor();
    protected static final SortMembersVisitor SORT_MEMBERS_VISITOR = new SortMembersVisitor();

    protected InitInnerClassVisitor.UpdateNewExpressionVisitor initInnerClassStep2Visitor = new InitInnerClassVisitor.UpdateNewExpressionVisitor();
    protected InitStaticFieldVisitor initStaticFieldVisitor = new InitStaticFieldVisitor();
    protected InitInstanceFieldVisitor initInstanceFieldVisitor = new InitInstanceFieldVisitor();
    protected InitEnumVisitor initEnumVisitor = new InitEnumVisitor();
    protected RemoveDefaultConstructorVisitor removeDefaultConstructorVisitor = new RemoveDefaultConstructorVisitor();
    protected UpdateBridgeMethodVisitor replaceBridgeMethodVisitor = new UpdateBridgeMethodVisitor();
    protected AddCastExpressionVisitor addCastExpressionVisitor;

    protected TypeDeclaration typeDeclaration;

    public UpdateJavaSyntaxTreeStep2Visitor(TypeMaker typeMaker) {
        this.addCastExpressionVisitor = new AddCastExpressionVisitor(typeMaker);
    }

    @Override
    public void visit(BodyDeclaration declaration) {
        ClassFileBodyDeclaration bodyDeclaration = (ClassFileBodyDeclaration)declaration;

        // Visit inner types
        if (bodyDeclaration.getInnerTypeDeclarations() != null) {
            TypeDeclaration td = typeDeclaration;
            acceptListDeclaration(bodyDeclaration.getInnerTypeDeclarations());
            typeDeclaration = td;
        }

        // Init visitor
        initStaticFieldVisitor.setInternalTypeName(typeDeclaration.getInternalTypeName());

        // Visit declaration
        initInnerClassStep2Visitor.visit(declaration);
        initStaticFieldVisitor.visit(declaration);
        initInstanceFieldVisitor.visit(declaration);
        removeDefaultConstructorVisitor.visit(declaration);
        AGGREGATE_FIELDS_VISITOR.visit(declaration);
        SORT_MEMBERS_VISITOR.visit(declaration);

        if (bodyDeclaration.getOuterBodyDeclaration() == null) {
            // Main body declaration

            if ((bodyDeclaration.getInnerTypeDeclarations() != null) && replaceBridgeMethodVisitor.init(bodyDeclaration)) {
                // Replace bridge method invocation
                replaceBridgeMethodVisitor.visit(bodyDeclaration);
            }

            // Add cast expressions
            addCastExpressionVisitor.visit(declaration);
        }
    }

    @Override
    public void visit(AnnotationDeclaration declaration) {
        this.typeDeclaration = declaration;
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(ClassDeclaration declaration) {
        this.typeDeclaration = declaration;
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(InterfaceDeclaration declaration) {
        this.typeDeclaration = declaration;
        safeAccept(declaration.getBodyDeclaration());
    }

    @Override
    public void visit(EnumDeclaration declaration) {
        this.typeDeclaration = declaration;

        // Remove 'static' and 'final' flags
        ClassFileEnumDeclaration cfed = (ClassFileEnumDeclaration)declaration;

        cfed.setFlags(cfed.getFlags() ^ (Declaration.FLAG_STATIC|Declaration.FLAG_FINAL));
        cfed.getBodyDeclaration().accept(this);
        initEnumVisitor.visit(cfed.getBodyDeclaration());
        cfed.setConstants(initEnumVisitor.getConstants());
    }
}
