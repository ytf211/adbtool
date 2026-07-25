package com.adbtool.process;

import java.util.List;

public interface ProcessRunner {
    ProcessResult run(List<String> command) throws Exception;
}
