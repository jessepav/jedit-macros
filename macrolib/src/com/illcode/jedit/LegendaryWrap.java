package com.illcode.jedit;

import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.textarea.JEditTextArea;

import org.apache.commons.lang3.StringUtils;
import org.gjt.sp.jedit.textarea.Selection;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class LegendaryWrap
{
    // MACRO START

    Pattern LIST_ITEM_PATTERN = Pattern.compile(" *(?:[\\*\\+\\-:~]|(?:\\d+|#)\\.) +");

    /**
     * Tests if a line is the start of some sort of list item, and if so, return the text
     * up until the end of the start pattern; otherwise return {@code null}.
     */
    String getListStartText(String s) {
        Matcher m = LIST_ITEM_PATTERN.matcher(s);
        if (m.lookingAt())
            return m.group();
        else
            return null;
    }

    /**
     * Returns the leading whitespace of a string.
     */
    String getLeadingWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isWhitespace(s.charAt(i)))
                return s.substring(0, i);
        }
        // If we reach here, the string was all whitespace
        return s;
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
        int parStartLine, parEndLine;
        String parText;

        int numLines = textArea.getLineCount();
        int currentLine = textArea.getCaretLine();

        // If the current line is all whitespace, there's nothing to be done
        if (StringUtils.isWhitespace(textArea.getLineText(currentLine))) {
            oa[0] = "";
            oa[1] = oa[2] = currentLine;
            return oa;
        }

        if (numLines == 1) {  // degenerate case
            oa[0] = textArea.getBuffer().getText();
            oa[1] = oa[2] = 0;
            return oa;
        }

        // First we scan up looking for the paragraph start line
        for (parStartLine = currentLine; parStartLine >= 0; parStartLine--) {
            String lineText = textArea.getLineText(parStartLine);
            if (getListStartText(lineText) != null) {
                break;
            } else if (StringUtils.isWhitespace(lineText)) {
                parStartLine++;   // the previously-scanned line is the paragraph start
                break;
            }
        }
        parStartLine = Math.max(parStartLine, 0);
        // Now scan down looking for the paragraph end line
        for (parEndLine = currentLine + 1; parEndLine < numLines; parEndLine++) {
            String lineText = textArea.getLineText(parEndLine);
            if (getListStartText(lineText) != null || StringUtils.isWhitespace(lineText)) {
                parEndLine--; // the previously-scanned line is the paragraph end
                break;
            }
        }
        parEndLine = Math.min(parEndLine, numLines - 1);
        int startOffset = textArea.getLineStartOffset(parStartLine);
        int endOffset = textArea.getLineEndOffset(parEndLine);
        // getLineEndOffset() always returns an offset 1 past the end of the
        // line's (non-newline) text, so we subtract 1 below
        parText = textArea.getText(startOffset, endOffset - startOffset - 1);
        oa[0] = parText;
        oa[1] = parStartLine;
        oa[2] = parEndLine;
        return oa;
    }

    void reflowText(String parText, StringBuilder sb, int wrapMargin) {
    }

    /**
     * This is the "main" method of the macro
     */
    void formatParagraph(View view, JEditTextArea textArea, Buffer buffer) {
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
        if (StringUtils.isWhitespace(parText))
            return;

        trailingNewline = parText.endsWith("\n");
        int tabSize = buffer.getIntegerProperty("tabSize", 8);
        parText.replace("\t", StringUtils.repeat(' ', tabSize));

        StringBuilder sb = new StringBuilder(parText.length() + 100);

        reflowText(parText, sb, buffer.getIntegerProperty("maxLineLen", 0));

        // Save our reformatted text back to parText
        if (trailingNewline)
            sb.append('\n');
        parText = sb.toString();

        int caretPos = textArea.getCaretPosition();

        // Finally, modify the text of the buffer
        if (usingSelection) {
            textArea.setSelectedText(parText);
        } else {
            int startOffset = textArea.getLineStartOffset(parStartLine);
            int endOffset = textArea.getLineEndOffset(parEndLine);
            textArea.setSelection(new Selection.Range(startOffset, endOffset));
            textArea.setSelectedText(parText);
        }

        textArea.setCaretPosition(Math.min(caretPos, buffer.getLength()));
    }

    // markdownFormatParagraph();
    // MACRO END
}
