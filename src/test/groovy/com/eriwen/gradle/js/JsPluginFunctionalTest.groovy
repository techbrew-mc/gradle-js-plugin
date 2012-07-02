package com.eriwen.gradle.js

import com.eriwen.gradle.js.util.FunctionalSpec

class JsPluginFunctionalTest extends FunctionalSpec {

    def setup() {
        buildFile << applyPlugin(JsPlugin)
    }

    def "basic processing chain"() {
        given:
        buildFile << """
            class TestTask extends SourceTask {

                @OutputDirectory
                File destination

                @TaskAction
                void doit() {
                    project.copy {
                        from getSource()
                        into destination
                    }
                }
            }

            javascript {
                source {
                    custom {
                        js {
                            srcDir "src/custom/js"
                        }
                        processing {
                            task(TestTask) {
                                destination project.file("build/\$name")
                            }
                            task("secondTask", TestTask) {
                                destination project.file("build/\$name")
                            }
                        }
                    }
                }
            }

            task copyProcessed(type: Copy) {
                from javascript.source.custom.processed
                into "build/out"
            }
        """
        and:
        file("src/custom/js/stuff.js") << ""

        when:
        launcher("copyProcessed").run()

        then:
        task("customTest").state.executed
        task("secondTask").state.executed

        when:
        launcher("copyProcessed").run()

        then:
        task("customTest").state.upToDate
        task("secondTask").state.upToDate
    }

    def "combine, minify, gzip"() {
        given:
        buildFile << """
            javascript.source {
                custom {
                    js {
                        srcDir "src/custom/js"
                    }
                }
            }

            combineJs {
                source = javascript.source.custom.js.files
                dest = file("\$buildDir/all.js")
            }

            minifyJs {
                source = combineJs
                dest = "\$buildDir/all-min.js" //Test flexible outputs
            }
        """
        and:
        file("src/custom/js/file1.js") << "function fn1() { console.log('1'); }"
        and:
        file("src/custom/js/file2.js") << "function fn2() { console.log('2'); }"

        when:
        run "minifyJs"

        then:
        file("build/all-min.js").text == "function fn1(){console.log(\"1\")}function fn2(){console.log(\"2\")};"

        and:
        wasExecuted ":combineJs" //Test inference
        wasExecuted ":minifyJs"

        when:
        run "minifyJs"

        then:
        wasUpToDate ":combineJs"
        wasUpToDate ":minifyJs"
    }
}
