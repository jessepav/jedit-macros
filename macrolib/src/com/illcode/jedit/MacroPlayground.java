package com.illcode.jedit;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.textarea.JEditTextArea;

/**
 * A class to use IDE code-completion to develop jEdit macros
 */
@SuppressWarnings("unchecked")
public class MacroPlayground
{
    Buffer buffer;
    View view;
    EditPane editPane;
    JEditTextArea textArea;
    DockableWindowManager wm;
    String scriptPath;

}
