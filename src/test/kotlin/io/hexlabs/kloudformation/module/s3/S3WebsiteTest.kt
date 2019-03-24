package io.hexlabs.kloudformation.module.s3

// import io.kloudformation.model.KloudFormationTemplate
// import io.kloudformation.module.value
// import io.kloudformation.toYaml
// import org.junit.jupiter.api.Test
// import kotlin.test.expect

class S3WebsiteTest {
//    @Test
//    fun `should have bucket and bucket policy by default`() {
//        val template = KloudFormationTemplate.create { s3Website { } }
//        template.toYaml()
//        ClassLoader.getSystemClassLoader().getResource("DefaultConfiguration.yml").readText()
//    }
//    @Test
//    fun `should return object with bucket and policy to reference`() {
//        KloudFormationTemplate.create {
//            val s3Website = s3Website { }
//            expect("Bucket") { s3Website.bucket.ref().ref }
//            expect("BucketPolicy") { s3Website.policy?.ref()?.ref }
//        }
//    }
//    @Test
//    fun `should remove policy if removed`() {
//        val template = KloudFormationTemplate.create {
//            val s3Website = s3Website { s3BucketPolicy { remove() } }
//            expect("Bucket") { s3Website.bucket.ref().ref }
//            expect(null) { s3Website.policy?.ref()?.ref }
//        }
//        template.toYaml()
//        ClassLoader.getSystemClassLoader().getResource("DefaultConfigurationWithoutPolicy.yml").readText()
//    }
//    @Test
//    fun `should update index document`() {
//        KloudFormationTemplate.create {
//            val s3Website = s3Website {
//                s3Bucket { modify {
//                    websiteConfiguration {
//                        indexDocument("abc.html")
//                    }
//                } }
//            }
//            expect("abc.html") { s3Website.bucket.websiteConfiguration?.indexDocument?.value() }
//        }
//    }
}