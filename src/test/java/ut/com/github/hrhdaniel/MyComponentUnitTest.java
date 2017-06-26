package ut.com.github.hrhdaniel;

import org.junit.Test;
import com.github.hrhdaniel.api.MyPluginComponent;
import com.github.hrhdaniel.impl.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest
{
    @Test
    public void testMyName()
    {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent",component.getName());
    }
}