package com.illcode.jedit;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.textarea.JEditTextArea;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class LegendaryWrap
{
    Buffer buffer;
    View view;
    EditPane editPane;
    JEditTextArea textArea;
    DockableWindowManager wm;
    String scriptPath;

    // MACRO START

    /**
     * Test if a character represents one of the punctuation characters that Pandoc Markdown
     * uses to indicate a list of some sort.
     */
    boolean isListPunctuation(char c) {
        return (c == '*' || c == '+' || c == '-' || c == ':' || c == '~' || c == '#');
    }

    Pattern NUMERIC_LIST_ITEM_PATTERN = Pattern.compile("[\\d#]\\.\\s");

    /**
     * Tests if a string indicates the start of an ordered list
     */
    boolean isOrderedListItem(String s) {
        Matcher m = NUMERIC_LIST_ITEM_PATTERN.matcher(s);
        return m.lookingAt();
    }

    /**
     * Tests if a line is the start of some sort of list item.
     */
    boolean isListStart(String s) {
        return isListPunctuation(s.charAt(0)) || isOrderedListItem(s);
    }

    /**
     * Checks if a line represents a paragraph boundary.
     */
    boolean isLineParagraphBoundary(String line) {
        return StringUtils.isWhitespace(line) || isListStart(line);
    }

    /**
     * This is the "main" method of the macro
     */
    void markdownFormatParagraph() {
        int parStartLine = -1, parEndLine = -1;
        String parText = null;

        String selectedText = textArea.getSelectedText();
        boolean usingSelection = selectedText != null;
        
    }

    // markdownFormatParagraph();
    // MACRO END
}
