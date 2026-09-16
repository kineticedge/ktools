dependencies {
//    implementation(project(":common"))
}

application {
    mainClass.set("io.kineticedge.tools.ToolCommand")
}

val copyScripts by tasks.register<Copy>("copyScripts") {
    from("src/scripts")
    into(layout.buildDirectory.dir("scripts"))
}

tasks.named("assemble") {
    dependsOn(copyScripts)
}

tasks.named<Tar>("distTar") {

    dependsOn(copyScripts)
}