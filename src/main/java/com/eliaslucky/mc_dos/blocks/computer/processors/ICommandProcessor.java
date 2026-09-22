package com.eliaslucky.furniture.blocks.computer.processors;

import com.eliaslucky.furniture.blocks.computer.ComputerBlockEntity;

public interface ICommandProcessor {
	String process(ComputerBlockEntity computer, String rawInput);
	String getPrompt(String currentPath);
}
