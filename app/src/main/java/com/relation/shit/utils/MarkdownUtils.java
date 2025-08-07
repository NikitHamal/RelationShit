package com.relation.shit.utils;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineBackgroundSpan;
import android.text.style.QuoteSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.LruCache;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Stack;

public class MarkdownUtils {

    // --- PATTERNS ---
    private static final Pattern INLINE_CODE_PATTERN = Pattern.compile("`+([^`]+?)`+");
    private static final Pattern BOLD_ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*\\*\\*(?!\\s)(.*?)(?<!\\s)\\*\\*\\*(?!\\*)");
    private static final Pattern BOLD_PATTERN = Pattern.compile("(?<!\\*)\\*\\*(?!\\s)(.*?)(?<!\\s)\\*\\*(?!\\*)");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\s)(.*?)(?<!\\s)\\*(?!\\*)");
    private static final Pattern STRIKETHROUGH_PATTERN = Pattern.compile("~~(?!\\s)(.*?)(?<!\\s)~~");
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*)");
    private static final Pattern BLOCKQUOTE_PATTERN = Pattern.compile("^>\\s?(.*)");
    private static final Pattern CODE_FENCE_PATTERN = Pattern.compile("^```.*");
    private static final Pattern UNORDERED_LIST_PATTERN = Pattern.compile("^(\\s*)([*+-])\\s+(.*)");
    private static final Pattern ORDERED_LIST_PATTERN = Pattern.compile("^(\\s*)(\\d+)\\.\\s+(.*)");
    private static final Pattern HR_PATTERN = Pattern.compile("^ {0,3}(?:[-*_]){3,}\\s*$");
    private static final Pattern ESCAPE_PATTERN = Pattern.compile("\\\\([*_~`])");

    // --- STYLE CONSTANTS ---
    private static final int BULLET_COLOR = Color.BLACK;
    private static final int BULLET_RADIUS = 5;
    private static final int BULLET_GAP_WIDTH = 28;
    private static final int LIST_INDENT_SIZE = 40;
    private static final int BLOCKQUOTE_INDENT = BULLET_GAP_WIDTH * 2;
    private static final int HR_COLOR = Color.LTGRAY;
    private static final int HR_HEIGHT = 2;

    // Cache for formatted Markdown strings
    private static final LruCache<String, Spanned> sParsingCache = new LruCache<>(50);

    /**
     * Custom span for ordered lists that displays numbers
     */
    private static class NumberSpan implements LeadingMarginSpan {
        private final String mNumber;
        private final int mMargin;
        private final int mColor;

        NumberSpan(int number, int margin, int color) {
            this.mNumber = number + ".";
            this.mMargin = margin;
            this.mColor = color;
        }

        @Override
        public int getLeadingMargin(boolean first) {
            return mMargin;
        }

        @Override
        public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, 
                                     int top, int baseline, int bottom, 
                                     CharSequence text, int start, int end, 
                                     boolean first, Layout layout) {
            if (first) {
                Paint.Style style = p.getStyle();
                int color = p.getColor();
                
                p.setColor(mColor);
                p.setStyle(Paint.Style.FILL);
                
                // Calculate position for right-aligned numbers
                float textWidth = p.measureText(mNumber);
                float pos = x + dir * (mMargin - textWidth - 5);
                
                c.drawText(mNumber, pos, baseline, p);
                p.setColor(color);
                p.setStyle(style);
            }
        }
    }

    /**
     * Custom span for drawing horizontal rules.
     */
    private static class HorizontalRuleSpan implements LineBackgroundSpan {
        private final int mColor;
        private final int mHeight;

        HorizontalRuleSpan(int color, int height) {
            mColor = color;
            mHeight = height;
        }

        @Override
        public void drawBackground(Canvas c, Paint p, int left, int right, int top, int baseline, int bottom, CharSequence text, int start, int end, int lnum) {
            int oldColor = p.getColor();
            Paint.Style oldStyle = p.getStyle();

            p.setColor(mColor);
            p.setStyle(Paint.Style.FILL);

            // Draw line in the middle of the line height
            int y = (top + bottom) / 2;
            c.drawRect(left, y - mHeight / 2, right, y + mHeight / 2, p);

            p.setColor(oldColor);
            p.setStyle(oldStyle);
        }
    }

    /**
     * Parses Markdown text and returns a Spanned string with styling.
     * @param text The Markdown text to format.
     * @return A Spanned string with Markdown styling.
     */
    public static Spanned formatMarkdown(String text) {
        if (text == null) return new SpannableStringBuilder("");
        
        // Normalize line endings for consistent processing
        String normalizedText = text.replace("\r\n", "\n").replace("\r", "\n");
        
        // Check cache with normalized text
        Spanned cachedResult = sParsingCache.get(normalizedText);
        if (cachedResult != null) {
            return cachedResult;
        }

        String[] lines = normalizedText.split("\n");
        SpannableStringBuilder builder = new SpannableStringBuilder();
        boolean inCodeBlock = false;
        
        // For nested lists
        Stack<Boolean> listTypeStack = new Stack<>(); 
        Stack<Integer> listIndentStack = new Stack<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            int lineStart = builder.length();

            // Handle code blocks
            if (CODE_FENCE_PATTERN.matcher(line).matches()) {
                inCodeBlock = !inCodeBlock;
                if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
                    builder.append("\n");
                }
                continue;
            }

            if (inCodeBlock) {
                builder.append(line).append(i < lines.length - 1 ? "\n" : "");
                builder.setSpan(new TypefaceSpan("monospace"), lineStart, builder.length(), 
                              Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                continue;
            }

            // --- Process non-code lines ---
            String processedLine = line;
            
            // Unescape special characters
            processedLine = unescapeMarkdown(processedLine);
            
            // Check for Horizontal Rule
            if (HR_PATTERN.matcher(line).matches()) {
                builder.append("\n");
                builder.append("\u200B"); // Zero-width space placeholder
                builder.setSpan(new HorizontalRuleSpan(HR_COLOR, HR_HEIGHT), 
                               lineStart, builder.length(), 
                               Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.append("\n\n");
                continue;
            }

            // --- Extract content for block elements ---
            Matcher headingMatcher = HEADING_PATTERN.matcher(line);
            Matcher blockquoteMatcher = BLOCKQUOTE_PATTERN.matcher(line);
            Matcher unorderedMatcher = UNORDERED_LIST_PATTERN.matcher(line);
            Matcher orderedMatcher = ORDERED_LIST_PATTERN.matcher(line);
            
            boolean isHeading = false;
            boolean isBlockquote = false;
            boolean isListLine = false;
            int currentLineIndent = 0;

            if (headingMatcher.matches()) {
                processedLine = headingMatcher.group(2); // Content without #
                isHeading = true;
            } else if (blockquoteMatcher.matches()) {
                processedLine = blockquoteMatcher.group(1); // Content without >
                isBlockquote = true;
            } else if (orderedMatcher.matches()) {
                isListLine = true;
                currentLineIndent = orderedMatcher.group(1).length();
                processedLine = orderedMatcher.group(3); // Content after "1. "
            } else if (unorderedMatcher.matches()) {
                isListLine = true;
                currentLineIndent = unorderedMatcher.group(1).length();
                processedLine = unorderedMatcher.group(3); // Content after "- "
            }

            // --- List Stack Handling ---
            // Pop stack until matching indentation level
            while (!listIndentStack.isEmpty() && currentLineIndent < listIndentStack.peek()) {
                listIndentStack.pop();
                listTypeStack.pop();
            }

            if (isListLine) {
                if (listIndentStack.isEmpty() || currentLineIndent > listIndentStack.peek()) {
                    // New list or deeper nested list
                    listIndentStack.push(currentLineIndent);
                    listTypeStack.push(orderedMatcher.matches());
                } else if (currentLineIndent == listIndentStack.peek()) {
                    // Same level list item, ensure type matches
                    if (listTypeStack.peek() != orderedMatcher.matches()) {
                        listTypeStack.pop();
                        listTypeStack.push(orderedMatcher.matches());
                    }
                }
            }

            // Append the processed line
            builder.append(processedLine);
            applyInlineStyles(builder, lineStart);

            // Apply block styles
            if (isHeading) {
                int level = headingMatcher.group(1).length();
                float size = 2.0f - (level * 0.2f);
                builder.setSpan(new RelativeSizeSpan(size), lineStart, builder.length(), 
                               Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new StyleSpan(Typeface.BOLD), lineStart, builder.length(), 
                               Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (isBlockquote) {
                builder.setSpan(new QuoteSpan(BULLET_COLOR), lineStart, builder.length(), 
                               Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new StyleSpan(Typeface.ITALIC), lineStart, builder.length(), 
                               Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new LeadingMarginSpan.Standard(BLOCKQUOTE_INDENT), 
                               lineStart, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (isListLine) {
                int totalIndent = (listIndentStack.size() - 1) * LIST_INDENT_SIZE + BULLET_GAP_WIDTH;
                if (listTypeStack.peek()) { // Ordered list
                    int number = Integer.parseInt(orderedMatcher.group(2));
                    builder.setSpan(new NumberSpan(number, totalIndent, BULLET_COLOR), 
                                   lineStart, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else { // Unordered list
                    builder.setSpan(new BulletSpan(totalIndent, BULLET_COLOR, BULLET_RADIUS), 
                                   lineStart, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            } else if (!listIndentStack.isEmpty() && !line.trim().isEmpty()) {
                // Continuation line - match list item text indent
                int totalIndent = (listIndentStack.size() - 1) * LIST_INDENT_SIZE + BULLET_GAP_WIDTH;
                builder.setSpan(new LeadingMarginSpan.Standard(totalIndent), 
                               lineStart, builder.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }

            // Add line breaks with spacing logic
            if (i < lines.length - 1) {
                builder.append("\n");
                if (isParagraphBreak(line, lines[i + 1])) {
                    builder.append("\n");
                }
            }
        }
        
        // Cache and return result
        sParsingCache.put(normalizedText, builder);
        return builder;
    }

    private static String unescapeMarkdown(String text) {
        return ESCAPE_PATTERN.matcher(text).replaceAll("$1");
    }

    private static boolean isList(String line) {
        return UNORDERED_LIST_PATTERN.matcher(line.trim()).matches() || 
               ORDERED_LIST_PATTERN.matcher(line.trim()).matches();
    }
    
    private static boolean isHeading(String line) {
        return HEADING_PATTERN.matcher(line.trim()).matches();
    }

    private static boolean isParagraphBreak(String currentLine, String nextLine) {
        if (currentLine.trim().isEmpty()) return false;
        if (isHeading(currentLine)) return true;
        if (isList(currentLine)) {
            return !isList(nextLine) && !nextLine.trim().isEmpty();
        }
        return true;
    }

    private static void applyInlineStyles(SpannableStringBuilder builder, int start) {
        applyStyle(builder, start, BOLD_ITALIC_PATTERN, new StyleSpan(Typeface.BOLD_ITALIC));
        applyStyle(builder, start, BOLD_PATTERN, new StyleSpan(Typeface.BOLD));
        applyStyle(builder, start, ITALIC_PATTERN, new StyleSpan(Typeface.ITALIC));
        applyStyle(builder, start, STRIKETHROUGH_PATTERN, new StrikethroughSpan());
        applyStyle(builder, start, INLINE_CODE_PATTERN, new TypefaceSpan("monospace"));
    }

    private static void applyStyle(SpannableStringBuilder builder, int start, 
                                  Pattern pattern, Object span) {
        String segment = builder.subSequence(start, builder.length()).toString();
        Matcher matcher = pattern.matcher(segment);

        while (matcher.find()) {
            int contentStart = matcher.start(1);
            int contentEnd = matcher.end(1);
            int matchStart = matcher.start();
            int matchEnd = matcher.end();

            builder.setSpan(cloneSpan(span), start + contentStart, start + contentEnd, 
                          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // Hide markdown markers
            builder.setSpan(new RelativeSizeSpan(0f), start + matchStart, 
                          start + contentStart, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            builder.setSpan(new RelativeSizeSpan(0f), start + contentEnd, 
                          start + matchEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private static Object cloneSpan(Object span) {
        if (span instanceof StyleSpan) {
            return new StyleSpan(((StyleSpan) span).getStyle());
        } else if (span instanceof TypefaceSpan) {
            return new TypefaceSpan(((TypefaceSpan) span).getFamily());
        } else if (span instanceof StrikethroughSpan) {
            return new StrikethroughSpan();
        }
        return span;
    }
}