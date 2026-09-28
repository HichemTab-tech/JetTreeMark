package com.github.hichemtabtech.jettreemark.toolwindow;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.StringReader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GitIgnoreMatcherTest {
    @Test
    public void appliesRulesInOrderIncludingNegation() throws Exception {
        GitIgnoreMatcher matcher = rules("", "*.log\n!important.log\nbuild/\n");

        assertTrue(matcher.isIgnored("service/debug.log", false));
        assertFalse(matcher.isIgnored("service/important.log", false));
        assertTrue(matcher.isIgnored("service/build", true));
    }

    @Test
    public void anchorsSlashPatternsToGitignoreDirectory() throws Exception {
        GitIgnoreMatcher matcher = rules("", "/logs\n");

        assertTrue(matcher.isIgnored("logs", true));
        assertFalse(matcher.isIgnored("service/logs", true));
    }

    @Test
    public void nestedRulesAreScopedAndOverrideParents() throws Exception {
        GitIgnoreMatcher parent = rules("", "*.tmp\n");
        GitIgnoreMatcher nested = parent.withRules("module", reader("!keep.tmp\n/generated/**\n"));

        assertFalse(nested.isIgnored("module/keep.tmp", false));
        assertTrue(nested.isIgnored("other/keep.tmp", false));
        assertTrue(nested.isIgnored("module/generated/file.bin", false));
        assertFalse(nested.isIgnored("other/generated/file.bin", false));
    }

    @Test
    public void supportsDoubleStarQuestionMarkAndCharacterClasses() throws Exception {
        GitIgnoreMatcher matcher = rules("", "docs/**/draft?.[mt]d\n");

        assertTrue(matcher.isIgnored("docs/a/b/draft1.md", false));
        assertTrue(matcher.isIgnored("docs/draft2.td", false));
        assertFalse(matcher.isIgnored("docs/a/draft10.md", false));
    }

    @Test
    public void handlesEscapedCommentNegationAndTrailingSpace() throws Exception {
        GitIgnoreMatcher matcher = rules("", "# comment\n\\#file\n\\!literal\nname\\ \n");

        assertTrue(matcher.isIgnored("#file", false));
        assertTrue(matcher.isIgnored("!literal", false));
        assertTrue(matcher.isIgnored("name ", false));
        assertFalse(matcher.isIgnored("name", false));
    }

    private static GitIgnoreMatcher rules(String base, String contents) throws Exception {
        return GitIgnoreMatcher.EMPTY.withRules(base, reader(contents));
    }

    private static BufferedReader reader(String contents) {
        return new BufferedReader(new StringReader(contents));
    }
}
