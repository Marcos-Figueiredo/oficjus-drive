"use client";

import { motion } from "framer-motion";
import { Mic, Navigation, CircleDot } from "lucide-react";

const STEPS = [
  {
    number: "01",
    icon: Mic,
    title: "DIGITA",
    description:
      "Endereços por CEP, logradouro ou comando de voz com as mãos no volante.",
    color: "cyan",
  },
  {
    number: "02",
    icon: Navigation,
    title: "INICIA",
    description:
      "O app calcula a rota otimizada instantaneamente e abre o Waze com um toque.",
    color: "lime",
  },
  {
    number: "03",
    icon: CircleDot,
    title: "BOLHA",
    description:
      "Registre e gerencie suas visitas diretamente pela bolha flutuante, sem nunca sair do GPS.",
    color: "cyan",
  },
];

export default function FlowSteps() {
  return (
    <section id="fluxo" className="section-padding relative">
      <div className="container-narrow">
        <div className="text-center mb-14">
          <motion.p
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            className="text-sm font-semibold tracking-widest text-cyan-electric uppercase mb-3"
          >
            Fluxo de uso
          </motion.p>
          <motion.h2
            initial={{ opacity: 0, y: 12 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            className="text-3xl sm:text-4xl font-bold text-white"
          >
            Três passos. Zero atrito.
          </motion.h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-6 lg:gap-8">
          {STEPS.map((step, i) => (
            <motion.div
              key={step.number}
              initial={{ opacity: 0, y: 24 }}
              whileInView={{ opacity: 1, y: 0 }}
              viewport={{ once: true }}
              transition={{ delay: i * 0.12 }}
              className="relative group"
            >
              {/* Connector line (desktop) */}
              {i < STEPS.length - 1 && (
                <div className="hidden md:block absolute top-12 left-[calc(50%+40px)] w-[calc(100%-80px)] h-px bg-gradient-to-r from-cyan-electric/40 to-transparent z-0" />
              )}

              <div className="relative glass rounded-2xl p-6 lg:p-8 border-gradient h-full transition-all duration-300 hover:shadow-glow">
                <div className="flex items-start gap-4">
                  <div
                    className={`flex-shrink-0 flex h-12 w-12 items-center justify-center rounded-xl ${
                      step.color === "cyan"
                        ? "bg-cyan-electric/15 text-cyan-electric"
                        : "bg-lime-neon/15 text-lime-neon"
                    }`}
                  >
                    <step.icon className="h-6 w-6" strokeWidth={2} />
                  </div>
                  <div>
                    <span className="text-xs font-mono text-space-300">
                      {step.number}
                    </span>
                    <h3 className="mt-1 text-xl font-bold text-white tracking-wide">
                      {step.title}
                    </h3>
                    <p className="mt-2 text-sm text-space-200 leading-relaxed">
                      {step.description}
                    </p>
                  </div>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      </div>
    </section>
  );
}
