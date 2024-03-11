package org.sonar.duplications.block;


import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class BlockMutationTest {

    @Test
    public void testGetHashHexWithNonEmptyHash() {
        // Creating a Block instance with a non-empty hash
        Block block = Block.builder()
                .setResourceId("exampleResource")
                .setBlockHash(new ByteArray(new byte[]{1, 2, 3})) // Passing a non-empty hash
                .setIndexInFile(1)
                .setLines(1, 5)
                .setUnit(1, 2)
                .build();

        // Asserting that getHashHex returns a non-empty string
        assertNotEquals("", block.getHashHex());
    }

    @Test
    public void testGetHashHexWithEmptyHash() {
        // Creating a Block instance with an empty hash
        Block block = Block.builder()
                .setResourceId("exampleResource")
                .setBlockHash(new ByteArray(new byte[0])) // Passing an empty hash
                .setIndexInFile(1)
                .setLines(1, 5)
                .setUnit(1, 2)
                .build();

        // Asserting that getHashHex returns an empty string
        assertEquals("", block.getHashHex());
    }
}
