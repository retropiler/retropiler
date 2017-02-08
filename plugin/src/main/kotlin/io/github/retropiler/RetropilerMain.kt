package io.github.retropiler

import java.io.File

class RetropilerMain(val inputFiles: List<File>, val outputDir: File) {
    fun run() {
        inputFiles.forEach { System.out.println("inputFile: $it") }
        System.out.println("outputDir: $outputDir")

        inputFiles.forEach { classFile ->
            //Files.copy()
        }

    }
}
