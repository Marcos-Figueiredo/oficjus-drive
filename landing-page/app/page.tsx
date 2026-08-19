import Header from "@/components/Header";
import Hero from "@/components/Hero";
import FlowSteps from "@/components/FlowSteps";
import BentoGrid from "@/components/BentoGrid";
import DatabaseSection from "@/components/DatabaseSection";
import TechStack from "@/components/TechStack";
import SecuritySection from "@/components/SecuritySection";
import PricingCTA from "@/components/PricingCTA";
import Footer from "@/components/Footer";

export default function HomePage() {
  return (
    <>
      <Header />
      <main>
        <Hero />
        <FlowSteps />
        <BentoGrid />
        <DatabaseSection />
        <TechStack />
        <SecuritySection />
        <PricingCTA />
      </main>
      <Footer />
    </>
  );
}
