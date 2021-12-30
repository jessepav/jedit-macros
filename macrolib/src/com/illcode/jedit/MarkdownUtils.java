package com.illcode.jedit;

import org.gjt.sp.jedit.jEdit;
import org.gjt.sp.jedit.Buffer;
import org.gjt.sp.jedit.View;
import org.gjt.sp.jedit.textarea.JEditTextArea;
import org.gjt.sp.jedit.textarea.Selection;

import org.apache.commons.lang3.StringUtils;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@SuppressWarnings("unchecked")
public final class MarkdownUtils
{
    // "(?: *(?:[\*\+\-:~]|(?:\d+|#)\.) +|\[\^\S{2})"
    public static final Pattern LIST_ITEM_PATTERN =
        Pattern.compile("(?: *(?:[\\*\\+\\-:~]|(?:\\d+|#)\\.) +|\\[\\^\\S{2})");
    
    /**
     * Tests if a line is the start of some sort of list item, and if so, return the text
     * up until the end of the start pattern; otherwise return {@code null}.
     */
    private String getListStartText(String s) {
        Matcher m = LIST_ITEM_PATTERN.matcher(s);
        if (m.lookingAt())
            return m.group();
        else
            return null;
    }
    
    /**
     * Returns true if a given line is a paragraph delimeter.
     */
    private boolean isParagraphDelimiter(String line) {
        int n = line.length();
        char firstChar = firstNonSpaceChar(line);
        return n == 0 ||   // empty line 
               ((firstChar == '<' || line.charAt(0) == ' ') && line.charAt(n-1) == '>') ||  // HTML tag
               StringUtils.isWhitespace(line) || // blank line
               (firstChar == ':' && line.trim().startsWith(":::")) || // Pandoc div
               (firstChar == '>' && line.trim().equals(">")); // paragraph break in blockquote  
    }
    
    /**
     * Return true if the given line is the start of a paragraph.
     */
    private boolean isParagraphStart(String line) {
        char c = firstNonSpaceChar(line);
        if (c == 0xA0 || c == 0x2003 || c == 0x2002) { // formatting indendation
            return true;
        } else if (c == '&') {
            line = line.trim();
            return line.startsWith("&nbsp;") || line.startsWith("&emsp;") || line.startsWith("&ensp;");
        } else {
            return false;
        }
    }
    
    /**
     * Return true if the given line is the last line of a paragraph.
     */
    private boolean isParagraphEnd(String line) {
        return line.endsWith("<br>") || line.endsWith("<br />") || line.endsWith("\\");
    }
    
    /**
     * Return the first non-space character in a line, or '\0' if the line is empty or has no
     * non-space characters.
     */
    private char firstNonSpaceChar(String line) {
        char c = '\0';
        int n = line.length();
        for (int i = 0; i < n; i++) {
            char c2 = line.charAt(i);
            if (c2 != ' ') {
                c = c2;
                break;
            }
        }
        return c;
    }

    /**
     * Returns the leading whitespace of a string.
     */
    private String getLeadingWhitespace(String s) {
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
     *   Index 0 - paragraph text (String)
     *   Index 1 - starting line of the paragraph (Integer)
     *   Index 2 - ending line of the paragraph (Integer)
     * </pre>
     * @return paragraph text and line boundaries
     */
    private Object[] getParagraphText(JEditTextArea textArea) {
        Object[] oa = new Object[3];
        int parStartLine, parEndLine;
        String parText;

        int numLines = textArea.getLineCount();
        int currentLine = textArea.getCaretLine();
        String trimmedLine = textArea.getLineText(currentLine).trim();

        // If the current line is all whitespace or an empty blockquote line, there's nothing to be done
        if (trimmedLine.isEmpty() || trimmedLine.equals(">")) {
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
            if (isParagraphDelimiter(lineText) || (parStartLine != currentLine && isParagraphEnd(lineText))) {
                if (parStartLine != currentLine)
                    parStartLine++;   // the previously-scanned line is the paragraph start
                break;
            } else if (getListStartText(lineText) != null || isParagraphStart(lineText)) {
                break;
            }
        }
        parStartLine = Math.max(parStartLine, 0);
        // Now scan down looking for the paragraph end line
        if (isParagraphEnd(textArea.getLineText(currentLine))) {
            parEndLine = currentLine;
        } else {
            for (parEndLine = currentLine + 1; parEndLine < numLines; parEndLine++) {
                String lineText = textArea.getLineText(parEndLine);
                if (getListStartText(lineText) != null || isParagraphDelimiter(lineText)) {
                    parEndLine--; // the previously-scanned line is the paragraph end
                    break;
                } else if (isParagraphEnd(lineText)) {
                    break;
                }
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

    /**
     * Used by formatParagraph() to reflow text.
     */
    private void reflowText(String parText, StringBuilder sb, int wrapMargin, boolean isBlockquote) {
        // The fraction by which words are allowed to overflow the margin
        int OVERFLOW_DIVISOR = jEdit.getIntegerProperty("JP.overflowDivisor", 4);

        String prefix = getListStartText(parText);
        if (prefix == null)
            prefix = getLeadingWhitespace(parText);
        int indent = prefix.length();
        String indentSpace = StringUtils.repeat(' ', indent);
        String text = parText.substring(indent).trim();
        String[] words = StringUtils.split(text);
        if (isBlockquote) {
            int pwsi = getLeadingWhitespace(prefix).length();  // prefix white-space indent
            prefix = StringUtils.repeat(' ', pwsi) + "> " + prefix.substring(pwsi);
            indentSpace = StringUtils.repeat(' ', pwsi) + "> " + StringUtils.repeat(' ', indent - pwsi);
            indent += 2;
        }
        int n = words.length;
        int col = 0;  // column at which we're inserting words
        sb.append(prefix).append(words[0]);
        col = indent + words[0].length(); 
        for (int i = 1; i < n; i++) {
            String word = words[i];
            int wordlen = word.length();
            int remaining = wrapMargin - col + 1;
            int overflow = wordlen + 1 - remaining;
            if (overflow < wordlen/OVERFLOW_DIVISOR) {
                sb.append(' ').append(word);
                col += 1 + wordlen;
            } else {  // go to a new line
                sb.append('\n').append(indentSpace).append(word);
                col = indent + wordlen;
            }
        }
    }
    
    /** Returns true if the first non-whitespace character in {@code text} is a {@code '>'}  */
    private boolean checkForBlockquote(String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!Character.isWhitespace(c))
                return c == '>';
        }
        return false;
    }
    
    // (?<=(?:^|\n) *)> ?
    public static final Pattern BLOCKQUOTE_PATTERN = Pattern.compile("(?<=(?:^|\\n) {0,8})> ?");
    
    /**
     * Reflow selected text (if a selection is active) or the current paragraph
     * to fit within the wrapMargin.
     *
     * If wrapMargin == 0, the margin will be taken from the JP.wrapLen buffer property, or, if 
                           that property is not defined, the buffer's maxLineLen property.
     * If wrapMargin == -1, the margin will be taken from the length of the first line of
     *                      the text to wrap.
     */
    public void formatParagraph(View view, JEditTextArea textArea, Buffer buffer, int wrapMargin) {
        int parStartLine = -1, parEndLine = -1;
        String parText;
        boolean usingSelection;
        boolean trailingNewline;

        String selectedText = textArea.getSelectedText();
        usingSelection = selectedText != null;
        
        int startOffset, endOffset, nonWhitespaceCount, extraSpaceCount; // used for caret positioning

        if (usingSelection) {
            parText = selectedText;
            startOffset = endOffset = nonWhitespaceCount = extraSpaceCount = -1;
        } else {
            Object[] parInfo = getParagraphText(textArea);
            parText = (String) parInfo[0];
            parStartLine = (Integer) parInfo[1];
            parEndLine = (Integer) parInfo[2];
            startOffset = textArea.getLineStartOffset(parStartLine);
            endOffset = textArea.getLineEndOffset(parEndLine) - 1;  // see comment in getParagraphText()

            // We are going to count the number of non-whitespace characters between the start of the
            // paragraph and the caret, to be used later in repositioning the caret.
            int caretPos = textArea.getCaretPosition();
            int caretOffset = caretPos - startOffset;
            nonWhitespaceCount = 0;
            for (int i = 0; i < caretOffset; i++) {
                if (!Character.isWhitespace(parText.charAt(i)))
                    nonWhitespaceCount++;
            }
            // If the caret is in the middle of the paragraph and is immediately after a space,
            // place it after the space in the wrapped paragraph too.
            extraSpaceCount = (caretPos != startOffset && caretPos != endOffset &&
                               parText.charAt(caretOffset-1) == ' ') ? 1 : 0;
        }
        if (StringUtils.isWhitespace(parText))
            return;

        trailingNewline = parText.endsWith("\n");
        int tabSize = buffer.getIntegerProperty("tabSize", 8);
        parText = parText.replace("\t", StringUtils.repeat(' ', tabSize));

        StringBuilder sb = new StringBuilder(parText.length() + 100);

        if (wrapMargin == 0) {
            wrapMargin = buffer.getIntegerProperty("JP.wrapLen", buffer.getIntegerProperty("maxLineLen", 72));
        } else if (wrapMargin == -1) {
            wrapMargin = parText.indexOf("\n");
            if (wrapMargin == -1)
                wrapMargin = buffer.getIntegerProperty("JP.wrapLen", buffer.getIntegerProperty("maxLineLen", 72));
        }
        boolean isBlockquote = checkForBlockquote(parText);
        if (isBlockquote) {
            parText = BLOCKQUOTE_PATTERN.matcher(parText).replaceAll("");
        }
        reflowText(parText, sb, wrapMargin, isBlockquote);

        // Save our reformatted text back to parText
        if (trailingNewline)
            sb.append('\n');
        parText = sb.toString();

        // Finally, modify the text of the buffer
        if (usingSelection) {
            textArea.setSelectedText(parText);
        } else {
            textArea.setSelection(new Selection.Range(startOffset, endOffset));
            textArea.setSelectedText(parText);
            
            // Reposition the caret based on the earlier tally of non-whitespace characters.
            int i = 0;
            while (nonWhitespaceCount > 0) {
                if (!Character.isWhitespace(parText.charAt(i++)))
                    nonWhitespaceCount--;
            }
            textArea.moveCaretPosition(startOffset + Math.min(i + extraSpaceCount, parText.length()));
        }
    }
    
    public void toggleBlockquote(View view, JEditTextArea textArea, Buffer buffer) {
        int caretLine = textArea.getCaretLine();
        int lineOffset = textArea.getLineStartOffset(caretLine);
        int caretCol = textArea.getCaretPosition() - lineOffset;
        String text = textArea.getSelectedText();
        if (text == null) {
            textArea.selectParagraph();
            text = textArea.getSelectedText();
        }
        if (text == null || text.isEmpty()) {
            textArea.setSelectedText("> ");
            return;
        }
        String[] lines = StringUtils.split(text, "\r\n");
        boolean addBlock = !checkForBlockquote(lines[0]);
        int tabSize = buffer.getIntegerProperty("tabSize", 8);
        text = text.replace("\t", StringUtils.repeat(' ', tabSize));
        if (addBlock) {
            // Let's get minimum indentation
            int minIndent = text.length();
            for (String line : lines)
                minIndent = Math.min(minIndent, getLeadingWhitespace(line).length());
            for (int i = 0; i < lines.length; i++)
                lines[i] = lines[i].substring(0, minIndent) + "> " + lines[i].substring(minIndent);
            textArea.setSelectedText(StringUtils.join(lines, '\n'));
        } else {
            textArea.setSelectedText(BLOCKQUOTE_PATTERN.matcher(text).replaceAll(""));
        }
        textArea.moveCaretPosition(textArea.getLineStartOffset(caretLine) + caretCol + (addBlock ? 2 : -2));
    }
}
