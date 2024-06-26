// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.jetbrains.python.run;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.Disposer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class PythonExtendedConfigurationEditor<T extends AbstractPythonRunConfiguration<T>> extends SettingsEditor<T> {
  private final @NotNull SettingsEditor<T> myMainSettingsEditor;

  private @Nullable PyRunConfigurationEditorFactory myCurrentEditorFactory;
  private @Nullable SettingsEditor<AbstractPythonRunConfiguration<?>> myCurrentSettingsEditor;
  private JComponent myCurrentSettingsEditorComponent;

  private JPanel mySettingsPlaceholder;

  public PythonExtendedConfigurationEditor(@NotNull SettingsEditor<T> editor) {
    myMainSettingsEditor = editor;

    Disposer.register(this, myMainSettingsEditor);
  }

  @Override
  protected void resetEditorFrom(@NotNull T s) {
    myMainSettingsEditor.resetFrom(s);
    updateCurrentEditor(s);
    if (myCurrentSettingsEditor != null) {
      myCurrentSettingsEditor.resetFrom(s);
    }
  }

  @Override
  protected void applyEditorTo(@NotNull T s) throws ConfigurationException {
    myMainSettingsEditor.applyTo(s);
    boolean updated = updateCurrentEditor(s);
    if (myCurrentSettingsEditor != null) {
      if (updated) {
        myCurrentSettingsEditor.resetFrom(s);
      }
      else {
        myCurrentSettingsEditor.applyTo(s);
      }
    }
  }

  private boolean updateCurrentEditor(T s) {
    PyRunConfigurationEditorFactory newEditorFactory = PyRunConfigurationEditorExtension.EP_NAME.getExtensionList().stream()
      .map(extension -> extension.accepts(s))
      .filter(Objects::nonNull)
      .findFirst()
      .orElse(null);
    if (!Objects.equals(myCurrentEditorFactory, newEditorFactory)) {
      // discard previous
      if (myCurrentSettingsEditorComponent != null) {
        mySettingsPlaceholder.remove(myCurrentSettingsEditorComponent);
      }
      if (myCurrentSettingsEditor != null) {
        Disposer.dispose(myCurrentSettingsEditor);
      }
      // set current editor
      myCurrentEditorFactory = newEditorFactory;
      myCurrentSettingsEditor = null;
      // add new
      if (newEditorFactory != null) {
        myCurrentSettingsEditor = newEditorFactory.createEditor(s);
        myCurrentSettingsEditorComponent = myCurrentSettingsEditor.getComponent();
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.weightx = 1.0;
        constraints.insets.top = 10;
        mySettingsPlaceholder.add(myCurrentSettingsEditorComponent, constraints);

        Disposer.register(this, myCurrentSettingsEditor);
      }
      return true;
    }
    return false;
  }

  @Override
  protected @NotNull JComponent createEditor() {
    JComponent mainEditorComponent = myMainSettingsEditor.getComponent();
    mySettingsPlaceholder = new JPanel(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weightx = 1.0;
    mySettingsPlaceholder.add(mainEditorComponent, constraints);
    return mySettingsPlaceholder;
  }

  public static @NotNull <T extends AbstractPythonRunConfiguration<T>> PythonExtendedConfigurationEditor<T> create(@NotNull SettingsEditor<T> editor) {
    return new PythonExtendedConfigurationEditor<>(editor);
  }
}
