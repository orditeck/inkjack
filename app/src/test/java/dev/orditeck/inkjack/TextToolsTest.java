package dev.orditeck.inkjack;

import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

public class TextToolsTest {
    @Test public void extractsUnicodeEntitiesWithoutTreatingEmailsAsHandles() {
        String input="Écris à kev@example.ca, @kév et #Québec. @kév";
        assertEquals(List.of("kev@example.ca"),TextTools.entities(input,TextTools.Kind.EMAIL));
        assertEquals(List.of("@kév"),TextTools.entities(input,TextTools.Kind.HANDLE));
        assertEquals(List.of("#Québec"),TextTools.entities(input,TextTools.Kind.HASHTAG));
    }
    @Test public void extractsPhoneAndLinksFromPunctuation() {
        assertEquals(List.of("https://example.ca/a"),TextTools.entities("See https://example.ca/a.",TextTools.Kind.URL));
        assertEquals(List.of("+1 (418) 555-0123"),TextTools.entities("Call +1 (418) 555-0123.",TextTools.Kind.PHONE));
    }
    @Test public void collectionPreservesIntentionalRepeatedLinesButDeduplicatesWholeCaptures() {
        var stash=new TextTools.Collection();
        assertTrue(stash.add(" hello\nhello "));
        assertFalse(stash.add("hello\nhello"));
        assertTrue(stash.add("world"));
        assertEquals("hello\nhello\n\nworld",stash.text());
        stash.clear();assertEquals(0,stash.size());assertEquals("",stash.text());
    }
    @Test public void collectionRejectsUnboundedGrowthWithoutLosingData() {
        var stash=new TextTools.Collection();stash.add("keep me");
        assertThrows(IllegalStateException.class,()->stash.add("x".repeat(TextTools.MAX_COLLECTION_CHARS)));
        assertEquals("keep me",stash.text());
    }
}
