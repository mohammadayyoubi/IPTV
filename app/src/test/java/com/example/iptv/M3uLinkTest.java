package com.example.iptv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.StringReader;

/** "Play a link" must tell a single stream apart from a channel playlist. */
public class M3uLinkTest {

    private static M3uLink.Result parse(String text, String base) throws Exception {
        M3uLink.Result result = new M3uLink.Result();
        M3uLink.parse(new BufferedReader(new StringReader(text)), base, result);
        return result;
    }

    @Test
    public void hlsMediaPlaylist_isAStream() throws Exception {
        // Al Jadeed's playlist shape: segments, no channels
        M3uLink.Result r = parse("#EXTM3U\n#EXT-X-TARGETDURATION:6\n#EXT-X-VERSION:3\n#EXTINF:5.52,\nseg1.ts\n",
                "http://host/chid_391/mono.m3u8");
        assertTrue(r.isStream);
        assertTrue(r.entries.isEmpty());
    }

    @Test
    public void hlsMasterPlaylist_isAStream() throws Exception {
        M3uLink.Result r = parse("#EXTM3U\n#EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360\nlow/index.m3u8\n",
                "https://host/live/master.m3u8");
        assertTrue(r.isStream);
    }

    @Test
    public void channelPlaylist_listsEntriesWithAttributesAndHeaders() throws Exception {
        String text = "#EXTM3U\n"
                + "#EXTINF:-1 tvg-name=\"Kanali 7\" tvg-logo=\"https://i.imgur.com/rL2v9pM.png\" group-title=\"Albania\",Kanali 7\n"
                + "https://fe.tring.al/delta/105/out/u/1200_1.m3u8\n"
                + "#EXTINF:-1 group-title=\"News\",Relative One\n"
                + "#EXTVLCOPT:http-user-agent=Mozilla/5.0 Test\n"
                + "#EXTVLCOPT:http-referrer=https://example.com/\n"
                + "streams/one.m3u8\n";
        M3uLink.Result r = parse(text, "https://lists.example.com/tv/playlist.m3u");
        assertFalse(r.isStream);
        assertEquals(2, r.entries.size());

        M3uLink.Entry first = r.entries.get(0);
        assertEquals("Kanali 7", first.name);
        assertEquals("https://i.imgur.com/rL2v9pM.png", first.logo);
        assertEquals("Albania", first.group);
        assertEquals("https://fe.tring.al/delta/105/out/u/1200_1.m3u8", first.url);
        assertNull(first.userAgent);

        M3uLink.Entry second = r.entries.get(1);
        assertEquals("Relative One", second.name);
        assertEquals("https://lists.example.com/tv/streams/one.m3u8", second.url); // resolved against the list
        assertEquals("Mozilla/5.0 Test", second.userAgent);
        assertEquals("https://example.com/", second.referrer);
    }

    @Test
    public void unknownContent_isTriedAsAStream() throws Exception {
        assertTrue(parse("<html>not a playlist</html>\n", "https://host/page").isStream);
    }
}
