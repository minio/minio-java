package io.minio;

import org.junit.Assert;
import org.junit.Test;

public class ListenBucketNotificationArgsTest {

    @Test
    public void testEmptyBuild() {
        String event = "abcde";
        ListenBucketNotificationArgs args1 = ListenBucketNotificationArgs
                .builder()
                .events(new String[]{new String(event)})
                .build();
        ListenBucketNotificationArgs args2 = ListenBucketNotificationArgs
                .builder()
                .events(new String[]{new String(event)})
                .build();
        Assert.assertEquals(args1, args2);
        Assert.assertEquals(args1.hashCode(), args2.hashCode());
    }

}
