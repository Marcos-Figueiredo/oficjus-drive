"use client";

import { useState, useEffect } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { Menu, X, MapPin } from "lucide-react";

const NAV_LINKS = [
  { href: "#recursos", label: "Recursos" },
  { href: "#tecnologia", label: "Tecnologia" },
  { href: "#seguranca", label: "Segurança" },
  { href: "#preco", label: "Preço" },
];

export default function Header() {
  const [scrolled, setScrolled] = useState(false);
  const [mobileOpen, setMobileOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <header
      className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${
        scrolled
          ? "bg-background/80 backdrop-blur-xl border-b border-white/5 shadow-lg"
          : "bg-transparent"
      }`}
    >
      <div className="container-narrow flex items-center justify-between h-16 px-4 sm:px-6 lg:px-8">
        {/* Logo */}
        <a href="#" className="flex items-center gap-2.5 group">
          <div className="relative flex h-9 w-9 items-center justify-center rounded-xl bg-cyan-electric/10 border border-cyan-electric/30 group-hover:border-cyan-electric/60 transition-colors">
            <MapPin className="h-5 w-5 text-cyan-electric" strokeWidth={2.5} />
            <span className="absolute -top-0.5 -right-0.5 h-2.5 w-2.5 rounded-full bg-lime-neon shadow-glow-lime" />
          </div>
          <span className="text-lg font-bold tracking-tight text-white">
            OficJus <span className="text-cyan-electric">Drive</span>
          </span>
        </a>

        {/* Desktop Nav */}
        <nav className="hidden md:flex items-center gap-8">
          {NAV_LINKS.map((link) => (
            <a
              key={link.href}
              href={link.href}
              className="text-sm font-medium text-space-200 hover:text-cyan-electric transition-colors"
            >
              {link.label}
            </a>
          ))}
        </nav>

        {/* CTA + Mobile Toggle */}
        <div className="flex items-center gap-3">
          <a
            href="#preco"
            className="hidden sm:inline-flex btn-primary text-sm py-2.5 px-5 animate-pulse-neon"
          >
            Testar Grátis por 7 dias
          </a>

          <button
            type="button"
            aria-label={mobileOpen ? "Fechar menu" : "Abrir menu"}
            className="md:hidden p-2 rounded-lg text-space-100 hover:bg-surface-elevated transition-colors"
            onClick={() => setMobileOpen((v) => !v)}
          >
            {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
      </div>

      {/* Mobile Menu */}
      <AnimatePresence>
        {mobileOpen && (
          <motion.div
            initial={{ opacity: 0, height: 0 }}
            animate={{ opacity: 1, height: "auto" }}
            exit={{ opacity: 0, height: 0 }}
            className="md:hidden border-t border-white/5 bg-background/95 backdrop-blur-xl"
          >
            <nav className="flex flex-col px-4 py-4 gap-1">
              {NAV_LINKS.map((link) => (
                <a
                  key={link.href}
                  href={link.href}
                  onClick={() => setMobileOpen(false)}
                  className="px-4 py-3 rounded-lg text-space-100 hover:bg-surface-elevated hover:text-cyan-electric transition-colors"
                >
                  {link.label}
                </a>
              ))}
              <a
                href="#preco"
                onClick={() => setMobileOpen(false)}
                className="mt-2 btn-primary text-center"
              >
                Testar Grátis por 7 dias
              </a>
            </nav>
          </motion.div>
        )}
      </AnimatePresence>
    </header>
  );
}
