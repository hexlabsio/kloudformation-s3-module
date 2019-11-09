package io.hexlabs.kloudformation.module.s3

import io.kloudformation.KloudFormation
import io.kloudformation.Value
import io.kloudformation.function.plus
import io.kloudformation.model.iam.PolicyDocument
import io.kloudformation.model.iam.Resource
import io.kloudformation.model.iam.action
import io.kloudformation.model.iam.policyDocument
import io.kloudformation.module.Module
import io.kloudformation.module.ModuleBuilder
import io.kloudformation.module.Properties
import io.kloudformation.module.builder
import io.kloudformation.module.modification
import io.kloudformation.module.optionalModification
import io.kloudformation.module.submodule
import io.kloudformation.resource.aws.s3.Bucket
import io.kloudformation.resource.aws.s3.BucketPolicy
import io.kloudformation.resource.aws.s3.bucket
import io.kloudformation.resource.aws.s3.bucketPolicy
import io.kloudformation.unaryPlus

data class S3Website(val bucket: Bucket, val policy: BucketPolicy? = null, val distribution: S3Distribution? = null) :
        Module {

    class Parts : io.kloudformation.module.Parts() {
        class BucketProps(var indexDocument: String = "index.html", var errorDocument: String = indexDocument) :
                Properties()
        class PolicyProps(var bucketRef: Value<String>, var policyDocument: PolicyDocument) : Properties()

        fun s3Distribution(
            domain: Value<String>,
            httpMethod: HttpMethod = HttpMethod.HTTP2,
            sslSupportMethod: SslSupportMethod = SslSupportMethod.SNI,
            priceClass: CloudfrontPriceClass = CloudfrontPriceClass._200,
            certificateArn: Value<String>? = null,
            modifications: S3Distribution.Parts.(S3Distribution.Predefined) -> Unit = {}
        ) {
            s3Distribution.invoke(S3Distribution.Props(domain, httpMethod, sslSupportMethod, priceClass, certificateArn), modifications)
        }
        val s3Bucket = modification<Bucket.Builder, Bucket, BucketProps>()
        val s3BucketPolicy = optionalModification<BucketPolicy.Builder, BucketPolicy, PolicyProps>()
        val s3Distribution = submodule { pre: S3Distribution.Predefined, props: S3Distribution.Props -> S3Distribution.Builder(pre, props) }
    }

    class Builder : ModuleBuilder<S3Website, Parts>(Parts()) {

        override fun KloudFormation.buildModule(): Parts.() -> S3Website = {
            val bucket = s3Bucket(Parts.BucketProps()) { props ->
                bucket {
                    accessControl(+"PublicRead")
                    websiteConfiguration {
                        indexDocument(props.indexDocument)
                        errorDocument(props.errorDocument)
                    }
                    modifyBuilder(props)
                }
            }
            val policyProps = Parts.PolicyProps(bucket.ref(), policyDocument {
                statement(
                    action = action("s3:GetObject"),
                    resource = Resource(listOf(+"arn:aws:s3:::" + bucket.ref() + "/*"))
                ) { allPrincipals() }
            })
            val policy = s3BucketPolicy(policyProps) { props ->
                bucketPolicy(
                    bucket = props.bucketRef,
                    policyDocument = props.policyDocument
                ) {
                    modifyBuilder(props)
                }
            }
            val distribution = build(s3Distribution, S3Distribution.Predefined(bucket.ref(), bucket.websiteConfiguration?.indexDocument ?: +"index.html"))
            S3Website(bucket, policy, distribution)
        }
    }
}

val s3Website = builder(S3Website.Builder())