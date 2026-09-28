package com.github.hichemtabtech.jettreemark.toolwindow;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Ordered, compiled gitignore rules. A matcher is immutable, so directory
 * traversals can cheaply share it until a nested .gitignore adds more rules.
 */
final class GitIgnoreMatcher {
    static final GitIgnoreMatcher EMPTY = new GitIgnoreMatcher(List.of());

    private final List<Rule> rules;

    private GitIgnoreMatcher(List<Rule> rules) {
        this.rules = rules;
    }

    GitIgnoreMatcher withRules(@NotNull String basePath, @NotNull BufferedReader reader) throws IOException {
        List<Rule> added = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            Rule rule = parseRule(normalizePath(basePath), line);
            if (rule != null) {
                added.add(rule);
            }
        }
        if (added.isEmpty()) {
            return this;
        }

        List<Rule> combined = new ArrayList<>(rules.size() + added.size());
        combined.addAll(rules);
        combined.addAll(added);
        return new GitIgnoreMatcher(Collections.unmodifiableList(combined));
    }

    boolean isIgnored(@NotNull String rootRelativePath, boolean directory) {
        String path = normalizePath(rootRelativePath);
        boolean ignored = false;
        for (Rule rule : rules) {
            if (rule.matches(path, directory)) {
                ignored = !rule.negated();
            }
        }
        return ignored;
    }

    private static Rule parseRule(String basePath, String sourceLine) {
        String line = stripUnescapedTrailingSpaces(sourceLine);
        if (line.isEmpty() || line.charAt(0) == '#') {
            return null;
        }

        boolean negated = line.charAt(0) == '!';
        if (negated) {
            line = line.substring(1);
        } else if (line.startsWith("\\!") || line.startsWith("\\#")) {
            line = line.substring(1);
        }

        if (line.isEmpty()) {
            return null;
        }

        boolean directoryOnly = endsWithUnescapedSlash(line);
        if (directoryOnly) {
            line = line.substring(0, line.length() - 1);
        }
        boolean anchored = line.startsWith("/");
        if (anchored) {
            line = line.substring(1);
        }
        if (line.isEmpty()) {
            return null;
        }

        boolean pathPattern = anchored || line.indexOf('/') >= 0;
        String regex = globToRegex(line);
        Pattern compiled;
        if (pathPattern) {
            compiled = Pattern.compile("^" + regex + "$");
        } else {
            compiled = Pattern.compile("^(?:.*/)?" + regex + "$");
        }
        return new Rule(basePath, compiled, negated, directoryOnly);
    }

    private static String stripUnescapedTrailingSpaces(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == ' ') {
            int backslashes = 0;
            for (int i = end - 2; i >= 0 && value.charAt(i) == '\\'; i--) {
                backslashes++;
            }
            if ((backslashes & 1) == 1) {
                return value.substring(0, end - 2) + " ";
            }
            end--;
        }
        return value.substring(0, end);
    }

    private static boolean endsWithUnescapedSlash(String value) {
        if (!value.endsWith("/")) {
            return false;
        }
        int backslashes = 0;
        for (int i = value.length() - 2; i >= 0 && value.charAt(i) == '\\'; i--) {
            backslashes++;
        }
        return (backslashes & 1) == 0;
    }

    private static String globToRegex(String glob) {
        StringBuilder regex = new StringBuilder(glob.length() * 2);
        for (int i = 0; i < glob.length(); i++) {
            char current = glob.charAt(i);
            if (current == '*') {
                if (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                    while (i + 1 < glob.length() && glob.charAt(i + 1) == '*') {
                        i++;
                    }
                    if (i + 1 < glob.length() && glob.charAt(i + 1) == '/') {
                        i++;
                        regex.append("(?:.*/)?");
                    } else {
                        regex.append(".*");
                    }
                } else {
                    regex.append("[^/]*");
                }
            } else if (current == '?') {
                regex.append("[^/]");
            } else if (current == '[') {
                int closing = glob.indexOf(']', i + 1);
                if (closing > i + 1) {
                    String content = glob.substring(i + 1, closing);
                    if (content.charAt(0) == '!') {
                        content = '^' + content.substring(1);
                    }
                    regex.append('[').append(content).append(']');
                    i = closing;
                } else {
                    regex.append("\\[");
                }
            } else if (current == '\\' && i + 1 < glob.length()) {
                appendRegexLiteral(regex, glob.charAt(++i));
            } else {
                appendRegexLiteral(regex, current);
            }
        }
        return regex.toString();
    }

    private static void appendRegexLiteral(StringBuilder regex, char value) {
        if (".(){}+^$|\\".indexOf(value) >= 0) {
            regex.append('\\');
        }
        regex.append(value);
    }

    private static String normalizePath(String value) {
        String normalized = value.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private record Rule(String basePath, Pattern pattern, boolean negated, boolean directoryOnly) {
        boolean matches(String rootRelativePath, boolean directory) {
            String scopedPath;
            if (basePath.isEmpty()) {
                scopedPath = rootRelativePath;
            } else {
                String prefix = basePath + '/';
                if (!rootRelativePath.startsWith(prefix)) {
                    return false;
                }
                scopedPath = rootRelativePath.substring(prefix.length());
            }

            if (directoryOnly && !directory) {
                return false;
            }
            return pattern.matcher(scopedPath).matches();
        }
    }
}
