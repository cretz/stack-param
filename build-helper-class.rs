extern crate bindgen;

use std::env;
use std::path::PathBuf;
use std::process::Command;

fn main() {
    let javalib_path = std::env::current_dir().expect("No current dir").join("javalib");
    let mut gradle_path = javalib_path.join("gradlew");
    if cfg!(target_os = "windows") {
        gradle_path.set_extension("bat");
    }

    println!("Starting Gradle at {}", gradle_path.to_string_lossy());

    let output = Command::new(gradle_path)
        .current_dir(javalib_path)
        .arg("--no-daemon")
        .arg(":native:classes")
        .output()
        .expect("Couldn't start gradle");

    println!("Gradle stdout: {}", String::from_utf8_lossy(&output.stdout));
    println!("Gradle stderr: {}", String::from_utf8_lossy(&output.stderr));
    assert!(output.status.success());


    let bindings = bindgen::builder()
        .header("src/jvmti_sys/wrapper.h")

        // We want jni defs from the jni-sys crate
        .raw_line("use jni_sys::*;")
        .whitelist_recursively(false)

        .whitelisted_type(".*JVMTI.*")
        .whitelisted_type(".*jvmti.*")
        .whitelisted_type("^jlocation")
        .whitelisted_type("^jthread.*")
        .whitelisted_type("^jniNativeInterface$")

        // This is not defined in jni-sys for some reason
        .whitelisted_type("^_?jrawMonitorID")

        .whitelisted_var(".*JVMTI.*")
        .whitelisted_var(".*jvmti.*")

        .derive_default(true)

        .generate()
        .expect("Unable to generate bindings");


    // Write the bindings to the $OUT_DIR/bindings.rs file.
    let out_path = PathBuf::from(env::var("OUT_DIR").unwrap());
    bindings
        .write_to_file(out_path.join("bindings.rs"))
        .expect("Couldn't write bindings!");
}
