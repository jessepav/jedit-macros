package com.illcode.jedit;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.EditPane;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.gui.DockableWindowManager;
import org.gjt.sp.jedit.textarea.JEditTextArea;

import org.apache.commons.lang3.StringUtils;
import org.gjt.sp.jedit.textarea.Selection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class LegendaryWrap
{
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
     * Gets the text and line boundary information about the paragraph that holds the caret. The format
     * of the returned Object array is:
     * <pre>
  Index 0 - paragraph text (String)
  Index 1 - starting line of the paragraph (Integer)
  Index 2 - ending line of the paragraph (Integer)
     * </pre>
     * @return paragraph text and line boundaries
     */
    Object[] getParagraphText(JEditTextArea textArea) {
        Object[] oa = new Object[3];

        return oa;
    }

    /**
     * This is the "main" method of the macro
     */
    void formatParagraph(JEditTextArea textArea, View view) {
        int parStartLine = -1, parEndLine = -1;
        String parText;
        boolean usingSelection;
        boolean trailingNewline;

        String selectedText = textArea.getSelectedText();
        usingSelection = selectedText != null;
        if (usingSelection) {
            parText = selectedText;
        } else {
            Object[] parInfo = getParagraphText(textArea);
            parText = (String) parInfo[0];
            parStartLine = (Integer) parInfo[1];
            parEndLine = (Integer) parInfo[2];
        }
        trailingNewline = parText.endsWith("\n");

        StringBuilder sb = new StringBuilder(parText.length() + 100);

        // Save our reformatted text back to parText
        if (trailingNewline)
            sb.append('\n');
        parText = sb.toString();

        // Finally, modify the text of the buffer
        if (usingSelection) {
            textArea.setSelectedText(parText);
        } else {
            int startOffset = textArea.getLineStartOffset(parStartLine);
            int endOffset = textArea.getLineEndOffset(parEndLine);
            textArea.setSelection(new Selection.Range(startOffset, endOffset));
            textArea.setSelectedText(parText);
        }
    }

    // markdownFormatParagraph();
    // MACRO END
}
