package io.hexlabs.kloudformation.module.s3

import io.kloudformation.KloudFormation
import io.kloudformation.Value
import io.kloudformation.function.plus
import io.kloudformation.model.KloudFormationTemplate.Builder.Companion.awsRegion
import io.kloudformation.module.Module
import io.kloudformation.module.OptionalModification
import io.kloudformation.module.Properties
import io.kloudformation.module.SubModuleBuilder
import io.kloudformation.module.optionalModification
import io.kloudformation.property.aws.certificatemanager.certificate.DomainValidationOption
import io.kloudformation.property.aws.cloudfront.distribution.CustomOriginConfig
import io.kloudformation.property.aws.cloudfront.distribution.DefaultCacheBehavior
import io.kloudformation.property.aws.cloudfront.distribution.DistributionConfig
import io.kloudformation.property.aws.cloudfront.distribution.ForwardedValues
import io.kloudformation.property.aws.cloudfront.distribution.Origin
import io.kloudformation.property.aws.cloudfront.distribution.ViewerCertificate
import io.kloudformation.resource.aws.certificatemanager.Certificate
import io.kloudformation.resource.aws.certificatemanager.certificate
import io.kloudformation.resource.aws.cloudfront.Distribution
import io.kloudformation.resource.aws.cloudfront.distribution
import io.kloudformation.unaryPlus

enum class CertificationValidationMethod { EMAIL, DNS }
enum class SslSupportMethod(val value: String) { SNI("sni-only"), VIP("vip") }
enum class HttpMethod(val value: String) { HTTP1_1("http1.1"), HTTP2("http2") }
enum class CloudfrontPriceClass(val value: String) { _100("PriceClass_100"), _200("PriceClass_200"), ALL("PriceClass_ALL") }

class S3Distribution(val certificate: Certificate?, val cloudfrontDistribution: Distribution?) : Module {

    class Predefined(
        var bucketRef: Value<String>,
        var rootObject: Value<String>
    ) : Properties()

    class Props(
        var domain: Value<String>,
        var httpMethod: HttpMethod = HttpMethod.HTTP2,
        var sslSupportMethod: SslSupportMethod = SslSupportMethod.SNI,
        var priceClass: CloudfrontPriceClass = CloudfrontPriceClass._200,
        var certificateArn: Value<String>? = null
    ) : Properties()

    class Parts : io.kloudformation.module.Parts() {
        val bucketCertificate: OptionalModification<Certificate.Builder, Certificate, CertificateProps> = optionalModification()
        val cloudFrontDistribution: OptionalModification<Distribution.Builder, Distribution, DistributionProps> = optionalModification()
    }

    data class CertificateProps(var domain: Value<String>, var validationMethod: CertificationValidationMethod = CertificationValidationMethod.DNS) : Properties()
    data class DistributionProps(var config: DistributionConfig) : Properties()

    class Builder(pre: Predefined, val props: Props) : SubModuleBuilder<S3Distribution, Parts, Predefined>(pre, Parts()) {

        override fun KloudFormation.buildModule(): Parts.() -> S3Distribution = {
            val certificate = if (props.certificateArn == null) {
                bucketCertificate(CertificateProps(props.domain)) { props ->
                    certificate(props.domain) {
                        subjectAlternativeNames(listOf(props.domain))
                        domainValidationOptions(listOf(DomainValidationOption(
                            domainName = props.domain,
                            validationDomain = props.domain
                        )))
                        validationMethod(props.validationMethod.toString())
                        modifyBuilder(props)
                    }
                }
            } else null

            val origin = Origin(
                id = +"s3Origin",
                domainName = pre.bucketRef + +".s3-website-" + awsRegion + +".amazonaws.com",
                customOriginConfig = CustomOriginConfig(
                    originProtocolPolicy = +"http-only"
                )
            )
            val distributionProps = DistributionProps(DistributionConfig(
                origins = listOf(origin),
                enabled = +true,
                aliases = +listOf(+"www." + props.domain, props.domain),
                defaultCacheBehavior = DefaultCacheBehavior(
                    allowedMethods = +listOf(+"GET", +"HEAD", +"OPTIONS"),
                    forwardedValues = ForwardedValues(queryString = +true),
                    targetOriginId = origin.id,
                    viewerProtocolPolicy = +"redirect-to-https"
                ),
                defaultRootObject = pre.rootObject,
                priceClass = +props.priceClass.value,
                httpVersion = +props.httpMethod.value,
                viewerCertificate = ViewerCertificate(acmCertificateArn = certificate?.ref() ?: props.certificateArn, sslSupportMethod = +props.sslSupportMethod.value)
            ))
            val cfDistribution = cloudFrontDistribution(distributionProps) { props ->
                distribution(props.config) {
                    modifyBuilder(props)
                }
            }
            S3Distribution(certificate, cfDistribution)
        }
    }
}